# Runbook de Deploy — produção

> Documento operacional da **Fase 12b** (concluída em 2026-09-12). Este arquivo descreve a
> arquitetura real em produção, o provisionamento, as variáveis de ambiente (por nome —
> o repositório é **público**, nunca versionar valores), os fluxos de deploy/rollback,
> a verificação pós-deploy e o troubleshooting. O backup/restauração de dados está em
> [`backup.md`](backup.md).

## 1. Arquitetura em produção

| Camada | Serviço (plano) | Detalhes |
|---|---|---|
| API (Spring Boot 4 / Java 21) | Render **Web Service** (free) | Dockerfile multi-stage; escuta na **8080** (EXPOSE); dorme após ~15 min de inatividade e acorda em ~1 min (cold start); health check `/actuator/health` |
| Banco (PostgreSQL) | Neon (free) | Conexão da aplicação via URL **pooled** (`-pooler`, pgBouncer) com `sslmode=require`; suspende após ~5 min de inatividade (dados preservados); URL **direct** para `psql`/`pg_dump` |
| Front (React + Vite) | Vercel (static) | Root dir `frontend`; build `vite build` (saída `dist`); SPA fallback via `vercel.json` |

Endpoints de produção:

- API: `https://gestao-compras-publicas-api.onrender.com` (prefixo de negócio em `/api`)
- Front: `https://gestao-compras-publicas.vercel.app`

## 2. Provisionamento inicial (uma vez)

### Neon (banco)
1. Criar o projeto no painel do Neon.
2. Obter as duas strings de conexão:
   - **Pooled** → usar em `SPRING_DATASOURCE_URL` (aplicação).
   - **Direct** → usar no `psql`/`pg_dump` (operações manuais).
3. Schema e migrations: o **Flyway** gerencia o schema (`ddl-auto=validate`). Em banco novo, o
   primeiro boot da API aplica `V1`–`V5` (arquivos em `src/main/resources/db/migration/`);
   alternativamente, aplicar manualmente via `psql` antes do boot. Conferir a tabela
   `flyway_schema_history` para validar o estado.

### Render (API)
1. Novo **Web Service** a partir do repositório backend (o `Dockerfile` é detectado automaticamente).
2. Definir as variáveis de ambiente da [tabela da seção 3](#3-variáveis-de-ambiente).
3. Health check do serviço: path `/actuator/health`.
4. Depois do primeiro boot, conferir `/actuator/health` (200) e que o perfil **prod** está ativo
   (Swagger desabilitado, SQL em INFO).

### Vercel (front)
1. Conectar o repo; configurar **Root Directory = `frontend`**; framework detectado como Vite.
2. Definir `VITE_API_URL` (ver seção 3).
3. `vercel.json` já cria o SPA fallback (rewrite de tudo para `/index.html`) — obrigatório para
   rotas profundas (`/app/empenhos`, etc.) funcionarem em recarga direta.
4. **Integração GitHub (GitHub App) do Vercel não está entregando eventos de push** neste repo
   (webhook ausente). Por isso, o caminho confiável de deploy é o **Deploy Hook** (seção 4).

## 3. Variáveis de ambiente

> Repositório **público**: abaixo estão apenas os **nomes**. Os valores (senha da Neon, `JWT_SECRET`,
> URL do Deploy Hook) ficam nos painéis / gestor de senhas e **nunca** no repositório.

**Render — API (`SPRING_PROFILES_ACTIVE=prod` é obrigatório):**

| Nome | Obrigatória | Observação |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | sim | `prod` — ativa `application-prod.properties` |
| `SPRING_DATASOURCE_URL` | sim | URL pooled do Neon + `?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | sim | usuário da Neon |
| `SPRING_DATASOURCE_PASSWORD` | sim | senha da Neon |
| `JWT_SECRET` | sim | chave de assinatura (ex.: `openssl rand -base64 48`); sem valor em prod o `JwtService` falha na inicialização (fail-fast) |
| `CORS_ALLOWED_ORIGINS` | sim (prod) | `https://gestao-compras-publicas.vercel.app` (origin do front) |
| `JWT_EXPIRATION_MS` | não | padrão 8h (28.800.000) |

**Vercel — front:**

| Nome | Obrigatória | Observação |
|---|---|---|
| `VITE_API_URL` | sim (prod) | API + `/api`: `https://gestao-compras-publicas-api.onrender.com/api` — o `api.ts` usa essa env (build-time) e cai para `/api` em dev |

Notas de config:
- `cors.allowed-origins` (default dev: `http://localhost:5173,http://localhost:3000`) é sobrescrito
  em produção pela env `CORS_ALLOWED_ORIGINS`.
- `application-prod.properties`: `ddl-auto=validate`, SQL em `INFO`, Actuator só `health/info` sem
  detalhes, Swagger/OpenAPI **desabilitado**.

## 4. Fluxos de deploy

### Front (Vercel)
1. `git push origin main` (CI roda: backend, front lint+build, publish GHCR).
2. **Atenção:** se o Vercel não disparar sozinho (a integração GitHub não entrega eventos aqui),
   usar o **Deploy Hook**:
   - Vercel → projeto → Settings → Git → Deploy Hooks (criado para a branch `main`).
   - Disparo: `curl -X POST <URL_DO_DEPLOY_HOOK>` (a URL contém token — guardar em gestor de senhas,
     não commitar).
   - Aguardar ~1 min e verificar o bundle novo (seção 5).

### API (Render)
- `git push origin main` → o Render deploya o branch automaticamente (ou "Manual Deploy" no painel).
- Confirmar: 200 em `/actuator/health`; perfil `prod`; logs de erro no painel se houver falha de
  migration (`validate` falha quando as entidades divergem do schema — nesse caso, gerar uma `V6`).

## 5. Verificação pós-deploy (checklist)

1. `curl https://gestao-compras-publicas-api.onrender.com/actuator/health` → `200` (pode ter cold start de ~1 min).
2. Login: `POST /api/auth/login` → `200` + token.
3. `GET /api/auth/me` → perfil e organizações com papel.
4. Swagger em prod **deve retornar 401** (`/swagger-ui.html` e `/v3/api-docs`).
5. CRUD: com papel ADMIN na org ativa, `POST /api/dotacoes` → `201` (regressão do bug `ehAdmin`).
6. Regra de competência: `POST /api/empenhos` na competência corrente → `201`; duplicada/futura → `409`.
7. Bundle do front: confirmar que o hash novo está servido em
   `https://gestao-compras-publicas.vercel.app/assets/index-*.js` usando `curl --compressed`
   (o caminho **sem** `/assets/` retorna `index.html` pelo SPA fallback e engana a verificação).
8. Fluxo completo automatizado: `scripts/test-api.sh` (smoke E2E, 45 verificações) com a URL da API
   de produção apontada.

## 6. Segurança e segredos

- Repositório **público**: nenhum valor sensível versionado.
- `JWT_SECRET` sem default em prod (fail-fast); os valores default no `application.properties`
  são somente de desenvolvimento.
- `DataInitializer` só cria usuários fora do profile `prod` — a senha padrão de dev não nasce em
  produção; usuários são criados pelo cadastro público (`POST /api/auth/register`, perfil global
  fixo `USUARIO`) e o primeiro ADMIN da organização é quem a cria.
- CORS `allowedHeaders` restrito a `Authorization, Content-Type, X-Org-Id`.

## 7. Backup, restore e rollback

- **Dados:** usar a conexão **direct** da Neon com `pg_dump`/restore conforme [`backup.md`](backup.md).
- **Front:** redeploy de um deploy anterior no Vercel (ou voltar o commit e re-disparar o hook).
- **API:** Render → "Deploy" de um commit anterior; se a mudança tocou schema, o Flyway pode exigir
  reverter com a migration devolvida (ou restore do banco).

## 8. Troubleshooting

| Sintoma | Causa provável | Solução |
|---|---|---|
| Primeira request demora ~1 min | Cold start do Render (free) | Aguardar; usar health com retry na verificação |
| Conexão com banco lenta/suspensa | Neon suspendeu após inatividade | Primeira consulta acorda; dados preservados |
| `401 de autenticação` | Token ausente/expirado (TTL 8h) ou `JWT_SECRET` trocado | Refazer login; manter `JWT_SECRET` estável |
| `400 "X-Org-Id deve ser um número válido"` | Header `X-Org-Id` ausente, vazio ou não-numérico | Enviar `X-Org-Id: <id>` numérico em chamadas de negócio |
| `403 "Você não é membro desta organização."` | `X-Org-Id` de outra org ou org inexistente | Usar um dos ids de `GET /api/auth/me` |
| `403 "sem permissão"` | Papel insuficiente (ex.: VISITANTE em escrita) | Conferir papel na org ativa |
| `409` | Duplicado (empenho na competência, CNPJ, edital) ou conflito de lock | Ajustar dados; tratar como estado esperado no fluxo |
| Bundle do front parece "velho" | Cache/roteamento SPA | Inspecionar por `/assets/index-*.js` com `--compressed` |
| Falha de boot com `validate` | Entidade divergente do schema | Criar nova migration Flyway (`V6__...`) |
| Preflight CORS 403 | Origin não liberada | Adicionar a origin exata em `CORS_ALLOWED_ORIGINS` |