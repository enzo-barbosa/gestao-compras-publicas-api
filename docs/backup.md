# Backup e Restauração do Banco de Dados

O banco de dados é um **PostgreSQL 15** executado em container Docker (`postgres:15-alpine`),
com o volume nomeado `pgdata` (dev) ou `pgdata_prod` (produção). O schema é versionado pelo
Flyway (`V1_*`, `V2_*`, `V3_*`), então **nunca** restaure com `ddl-auto=create` — o Flyway valida
e reconcilia o schema automaticamente.

## Visão geral da estratégia

- **Tipo:** backup lógico (`pg_dump`, formato `custom`) — portável e restaura apenas a base, sem
  depender do serviço parado.
- **Frequência recomendada:** diário em produção (agendado via cron/systemd).
- **Retenção:** manter os últimos 14 dumps; reter o dump do 1º dia do mês por 12 meses.
- **Restauração periódica (drill):** restaure um dump em banco descartável pelo menos a cada
  90 dias para validar o procedimento.

## Backup manual

Desenvolvimento (container `gestao_compras_db`):

```bash
mkdir -p backups
docker compose exec db pg_dump -U postgres -d gestao_compras \
  --format=custom --no-owner --no-privileges \
  -f /var/lib/postgresql/data/gestao-compras-$(date +%F).dump
docker cp gestao_compras_db:/var/lib/postgresql/data/gestao-compras-$(date +%F).dump backups/
```

Produção (compose `docker-compose.prod.yml`):

```bash
docker compose -f docker-compose.prod.yml exec db pg_dump -U postgres -d gestao_compras \
  --format=custom --no-owner --no-privileges \
  -f /tmp/gestao-compras-$(date +%F%H%M).dump
docker cp gestao_compras_prod_db:/tmp/gestao-compras-*.dump ./backups/
```

> GZIP opcional: `gzip` o arquivo `.dump` (pg_dump custom já é compacto; gzip reduz adicionalmente).

## Restauração

Em banco **vazio** (criar depois que o container sobe):

```bash
# 1) Crie um banco vazio onde o dump será restaurado
docker compose exec db createdb -U postgres gestao_compras_restore

# 2) Copie o dump para o container
docker cp backups/gestao-compras-2026-09-07.dump gestao_compras_db:/tmp/restore.dump

# 3) Restaure (drop/criar no --clean garante consistência)
docker compose exec db pg_restore -U postgres -d gestao_compras_restore \
  --clean --if-exists --no-owner --no-privileges /tmp/restore.dump

# 4) Aponte o app para o banco restaurado e suba-o uma vez para o Flyway validar
```

> O Flyway usa `baseline-on-migrate=true, baseline-version=1` no `application.properties`: ao
> restaurar, as migrations já existentes (V1–V3) serão detectadas via schema history e nada será
> reaplicado. Para fins de teste, você pode também rodar
> `./mvnw flyway:validate` após o restore.

## Automação (produção)

Exemplo de job diário com systemd:

```ini
# /etc/systemd/system/gestao-compras-backup.service
[Unit]
Description=Backup diário do banco gestao-compras
After=docker.service

[Service]
Type=oneshot
WorkingDirectory=/opt/gestao-compras
ExecStart=/opt/gestao-compras/scripts/backup.sh
```

```ini
# /etc/systemd/system/gestao-compras-backup.timer
[Unit]
Description=Roda o backup diário às 03:00

[Timer]
OnCalendar=*-*-* 03:00:00
Persistent=true

[Install]
WantedBy=timers.target
```

`scripts/backup.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail
DIR=/opt/gestao-compras/backups
mkdir -p "$DIR"
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U postgres -d gestao_compras --format=custom --no-owner --no-privileges \
  | gzip > "$DIR/gestao-compras-$(date +%F).dump.gz"
find "$DIR" -name 'gestao-compras-*.dump.gz' -mtime +14 -delete
```

## Restauração de teste (drill em 30min)

1. Suba um banco Postgres 15 descartável: `docker run --rm -d -p 55432:5432 -e POSTGRES_PASSWORD=postgres postgres:15-alpine`.
2. Restaure o dump nele (mesmos comandos do fluxo acima, porta `55432`).
3. Rode a API apontando para a porta 55432: `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:55432/gestao_compras ./mvnw spring-boot:run`.
4. Valide: `GET /actuator/health` com `status: "UP"` e contagens de registros coerentes.
5. Destrua o container de teste.

## Varredura de segurança de imagem (Trivy)

O artefato de produção é a imagem Docker da API (`API_IMAGE`). Antes de publicar:

```bash
trivy image --severity HIGH,CRITICAL --ignore-unfixed \
  --exit-code 1 --no-progress "$API_IMAGE"
```

Gatilho recomendado: executar no CI a cada build de release, além de um scan agendado
(semanal) sobre a imagem em execução. O exit-code 1 faz o pipeline falhar quando existirem
vulnerabilidades HIGH/CRITICAL sem correção disponível no base image.

> Referências: o projeto usa `postgres:15-alpine`, `node:*-alpine` (build do web) e uma imagem
> JRE (distroless/azul-alpine) para a API — scannear também as imagens base.