# 📌 GUIA MESTRE — Sistema de Gestão de Compras Públicas

> **GUIA DE DESENVOLVIMENTO — versionado no repositório em `docs/guia-de-desenvolvimento.md`**
>
> ## 🤖 INSTRUÇÕES PARA O AGENTE DE IA (LEIA PRIMEIRO)
> 1. Leia este arquivo INTEIRO no início de cada sessão antes de escrever qualquer código.
> 2. Trabalhe APENAS na primeira fase com checkboxes pendentes (`- [ ]`), na ordem.
> 3. Ao concluir uma etapa, marque `- [x]` e atualize a seção **Log de Sessões** ao final.
> 4. NUNCA refaça etapas já concluídas. NUNCA pule etapas sem alinhar com o usuário.
> 5. Respeite as decisões arquiteturais fixas (seção abaixo). Não mude stack/versões sem perguntar.
> 6. Documentação pode usar `.md` normalmente; diagramas usam `.mermaid` e cenários de aceite usam `.feature`.
> 7. Ao final da sessão, resuma ao usuário o que foi feito e atualize este arquivo.
> 8. Commits apenas quando o usuário pedir explicitamente.

---

## 1. CONTEXTO DO NEGÓCIO

Sistema web full-stack que corrige uma falha real observada em prefeitura: contratos de 12 meses eram **empenhados integralmente à vista**, travando ~R$ 1 milhão/ano em dotações orçamentárias (fichas).

**Solução**: rateio mensal automático — cada empenho compromete apenas `valorTotal / duracaoMeses`, validando saldo da dotação ANTES de empenhar, tudo dentro de uma transação.

### Regra de negócio central (geração de empenho)
1. Usuário seleciona contrato + mês/ano de referência
2. Backend calcula `valorMensal = valorTotal / duracaoMeses`
3. Valida: `dotacao.saldoAtual >= valorMensal` E `contrato.saldoRestante >= valorMensal`
4. Se OK (transação): cria Empenho → debita dotação → debita saldoRestante do contrato → retorna 201
5. Se não: retorna 400 com erro claro

### Entidades e relacionamentos
- **DotacaoOrcamentaria**: codigo, descricao, saldoInicial, saldoAtual, anoExercicio | métodos debitar/creditar
- **CreditoSuplementar**: valor, origem, data, dotacaoDestino (remanejamento entre fichas)
- **Fornecedor**: nome, cnpj, email, telefone, endereco
- **Licitacao**: modalidade, numeroEdital, datas, status, valorEstimado, fornecedor vencedor
- **Contrato**: numero, objeto, valorTotal, duracaoMeses, dataInicio, status, saldoRestante; vinculado a dotacao + licitacao + fornecedor
- **Empenho**: dataEmpenho, mesReferencia, anoReferencia, valor, status; vinculado a contrato + usuario
- **Usuario**: nome, email, senha (BCrypt), perfil (ADMIN | USUARIO)

---

## 2. STACK E DECISÕES FIXAS

| Item | Decisão |
|---|---|
| Java | 21 ✅ instalado (OpenJDK 21.0.11) |
| Spring Boot | **4.1.1** (mantido — ATENÇÃO: starters com nomes novos, ex.: `spring-boot-starter-webmvc`; segurança via `SecurityFilterChain` bean) |
| Maven | Via wrapper `./mvnw` (3.9.16) — NÃO há mvn global |
| Banco | PostgreSQL 15 via Docker Compose |
| JWT | Biblioteca jjwt (`io.jsonwebtoken:jjwt-api/impl/jackson` 0.12.x) |
| Frontend | React 19 + TypeScript (strict) + Vite, Axios, monorepo na pasta `frontend/` |
| Deploy | Docker + GitHub Actions + AWS (EC2 t2.micro, RDS, ECR, S3) |
| Repositório | `git@github.com:enzo-barbosa/gestao-compras-publicas-api.git`, branch `main` |
| Camadas backend | controller → service → repository → model / dto / config, pacote raiz `com.gestaocompras` |
| Convenção | DTOs para entrada/saída (nunca expor entidade direto); validações com Bean Validation; Lombok disponível |

---

## 3. ESTADO ATUAL DO PROJETO

**Última atualização deste arquivo: ver Log de Sessões.**

Existe hoje:
- [x] Scaffold Spring Boot 4.1.1 (pom.xml com JPA, Security, Validation, WebMvc, PostgreSQL, Lombok)
- [x] `GestaoComprasPublicasApplication.java` + teste básico
- [x] `./mvnw` funcional (.mvn/wrapper restaurado)
- [x] Build compila (`./mvnw compile` OK)
- [x] `.gitignore` completo (Java/Node/IDE/env + markdown exceto README)
- [x] Git configurado, branch `main`, remoto GitHub OK
- [x] Java 21, Node v24.18.0, npm 11.16.0, Git 2.53.0 instalados
- [x] Docker CE 29.7.2 + Compose v5.5.0 instalados e funcionando sem sudo
- [x] `docker-compose.yml` criado (PostgreSQL 15-alpine, volume, healthcheck)
- [x] `application.properties` configurado (datasource + JPA + logs SQL)

Ainda NÃO existe:
- [ ] Deploy em nuvem (Fase 12b — destino a decidir; CI com publish no GHCR já pronto)

### Melhorias concluídas (programa de 8 fases — sessão 2026-09-07)

- [x] Schema gerenciado por **Flyway** (migrations `V1__init`, `V2__add_indexes_and_fix_empenho_unique`, `V3__add_version_columns`, `V4__organizacoes_multitenancy`) com `baseline-on-migrate`; `ddl-auto=validate` em todos os profiles
- [x] **Locking financeiro**: colunas `version` em `dotacoes`/`contratos` (otimista) + lock pessimista `PESSIMISTIC_WRITE` no budget; `ObjectOptimisticLockingFailureException` → 409 com mensagem amigável
- [x] **Perf de listagens**: `@EntityGraph` nos 4 repositórios principais (paginação sem N+1); `DotacaoService.debitar/creditar` retornam a entidade (elimina re-busca redundante)
- [x] **Auth aprimorada**: endpoint `GET /api/auth/me`; frontend valida a sessão no boot e redireciona com aviso de sessão expirada; `JwtService` com fail-fast em secret em branco; TTL do token 24h → 8h (`JWT_EXPIRATION_MS`)
- [x] **Frontend refatorado**: TypeScript strict (`noUncheckedIndexedAccess`), hook `useCrudPage<T>`, sistema de toasts, paginação tipada `Pagina<T>`, `overflow-x` em tabelas, validação client (CNPJ 14 dígitos, encerramento ≥ abertura, valor/duração)
- [x] **Testes**: backend **94/94** (novo `GlobalExceptionHandlerTest` com MockMvc standalone + `ActuatorSecurityTest`); frontend **23/23** com Vitest (`utils/format` + `utils/validacao`)
- [x] **Operações**: Spring Boot Actuator (`/actuator/health` público, demais endpoints só ADMIN, sem detalhes em prod); `docs/backup.md` (backup/restore pg_dump, automação systemd, drill de restauração, Trivy)

### Programa multitenancy por grupos (5 fases) — sessão 2026-09-07 (Fase 1 implementada)

- [x] **Fase 1 (dados + segurança)**: migration **`V4__organizacoes_multitenancy.sql`** cria `organizacoes`, `membros_organizacao` (papel por grupo `ADMIN`/`OPERADOR`/`VISITANTE`) e `convites_organizacao` (e-mail **ou** código); adiciona `organizacao_id` às 6 entidades de negócio + `creditos_suplementares`; uniques globais de V1 (dotação/código, contrato/número, fornecedor/cnpj, licitação/edital) migradas para **uniques compostas por organização**; perfil do usuário vira `SUPER_ADMIN|USUARIO` (`SUPER_ADMIN` global oculto, para auditoria). A ordem do SQL importa (perfil: DROP constraint anterior → UPDATE → ADD; uniques: `DROP CONSTRAINT IF EXISTS` antes de recriar compostas — banco baselined não tinha os nomes de V1). `organizacao_id` nasce nullable na migration (backfill real em prod) e recebe `SET NOT NULL` em runtime quando não-prod (em prod fica como operação manual pós-migração dos dados).
- [x] **Isolamento no backend**: serviços recebem `Long organizacaoId` como 1º parâmetro; controllers repassam o `organizacaoId()` do principal (`@AuthenticationPrincipal UsuarioLogado`); repositórios ficam org-escopados (`findByIdAndOrganizacaoId`, `existsByXAndOrganizacaoId`, specs com `root.get("organizacao").get("id")`); creates usam `organizacaoRepository.getReferenceById(orgId)`.
- [x] **Segurança**: `X-Org-Id` no header escolhe a organização ativa; SUPER_ADMIN → `ROLE_SUPER_ADMIN`; membro → `ROLE_<papel>`; sem header → sem authorities; não-membro → `403` "Você não é membro desta organização."; `X-Org-Id` inválido → `400`. Matriz: leitura `ADMIN|OPERADOR|VISITANTE|SUPER_ADMIN`, escrita `ADMIN|OPERADOR|SUPER_ADMIN`, register e actuator `ADMIN|SUPER_ADMIN`. `MovimentacaoDotacao` fica **sem** `organizacao_id` (acesso só aninhado); unique parcial de empenho da V2 mantida; `DataInitializer` mantém admin global como `SUPER_ADMIN` + `ADMIN` da org "Minha Organização" (id 1), que recebe os dados existentes.
- [x] **Testes**: 6 serviços reescritos com `@Mock OrganizacaoRepository` e stub lenient `getReferenceById` no setUp (**69/69** unitários); `AuthIntegrationTest` reescrito (admin `SUPER_ADMIN`, `X-Org-Id: 1`, comum sem membrosia → 403, fluxo de empenho pelo admin) + novo `IsolamentoOrganizacaoIntegrationTest` (operação cruzando orga → 404; não-membro → 403; VISITANTE lê e não escreve; OPERADOR escreve) → **96/96** verdes, BUILD SUCCESS.
- Pendências da Fase 1: commit do usuário (comandos na seção 7). Fases 2–5 em aberto (F2: API de grupos/membros/convites + `register` público; F3: frontend onboarding/seletor; F4: gestão de membros + painel SUPER_ADMIN; F5: fechamento de docs/scripts).

⚠️ **Testes e app exigem o Postgres rodando** (`docker compose up -d`) — o contexto Spring conecta no banco para o `ddl-auto`.

---

## 4. FASES DO PROJETO

### Fase 0 — Ambiente ⚙️
- [x] Java 21 instalado e validado
- [x] Node.js 20+ / npm instalados e validados
- [x] Git instalado e configurado
- [x] Maven wrapper (`./mvnw`) restaurado e validado
- [x] Build do scaffold compila sem erros
- [x] `.gitignore` criado na raiz
- [x] **Docker CE + compose-plugin instalados** (Docker 29.7.2 + Compose v5.5.0)
- [x] `docker run hello-world` validado
- [x] Usuário adicionado ao grupo `docker` (daemon acessível sem sudo)

### Fase 1 — Configuração base 🔧
- [x] Criar `docker-compose.yml` na raiz (PostgreSQL 15: porta 5432, volume, healthcheck, db=`gestao_compras`, user=`postgres`, password definida)
- [x] Configurar `application.properties` (datasource localhost:5432, JPA `ddl-auto=update` durante dev, logs SQL em dev)
- [x] Subir banco: `docker compose up -d` e testar conexão rodando a app (Hikari conectou; app iniciou em 3.6s)
- [x] Definir estratégia de profiles (default = dev; `prod` sobrescreve `ddl-auto` para `validate` — ver `application-prod.properties`)

### Fase 2 — Estrutura base + Usuario 👤
- [x] Criar pacotes: `model`, `repository`, `service`, `controller`, `dto`, `config` (+ `exception`)
- [x] Entidade `Usuario` (+ enum Perfil: ADMIN, USUARIO) — perfil com `@Enumerated(EnumType.STRING)`
- [x] `UsuarioRepository` (`findByEmail`, `existsByEmail`)
- [x] Tratamento global de erros (`@RestControllerAdvice` + DTO de erro padronizado: 400 validação, 404 NotFound, 409 integridade, 500 genérico)
- [x] Seed de usuário admin/admin via `CommandLineRunner` (email `admin@admin.com`, senha BCrypt, idempotente)
- [x] SecurityConfig TEMPORÁRIO (permitAll stateless + PasswordEncoder bean) — substituir pelo JWT na Fase 9

### Fase 3 — Módulo Orçamentário: Dotações 💰
- [x] Entidade `DotacaoOrcamentaria` (saldoInicial, saldoAtual, anoExercicio, codigo único) + métodos de domínio `debitar`/`creditar` (nunca negativo)
- [x] `DotacaoRepository`
- [x] `DotacaoService` (CRUD + regras: saldoAtual nunca negativo; PUT nunca altera saldos; DELETE bloqueado com histórico)
- [x] DTOs (Request/Response)
- [x] `DotacaoController` CRUD completo: `GET/POST/PUT/DELETE /api/dotacoes`
- [x] Endpoint de consulta de saldo e movimentações (entidade `MovimentacaoDotacao` criada nesta fase, com histórico imutável — base p/ Fases 4 e 8)
- [x] Testes de service (JUnit + Mockito — 9 testes)

### Fase 4 — Créditos Suplementares 🔄
- [x] Entidade `CreditoSuplementar` (valor, origem, data, dotacaoDestino)
- [x] Service: transação — debita dotação origem, credita destino, registra histórico (reusa `DotacaoService`, movimentações DEBITO/CREDITO_SUPLEMENTAR nos dois lados)
- [x] Validação: saldo origem suficiente (+ origem ≠ destino, dotações existentes)
- [x] Controller: `POST /api/creditos-suplementares` + listagem por dotação/período (`GET ?dotacaoId=&dataInicio=&dataFim=`)
- [x] Testes (4 unitários: sucesso, origem=destino, not found, saldo insuficiente sem registro)

### Fase 5 — Fornecedores 🏢
- [x] Entidade `Fornecedor` (CNPJ único armazenado só com dígitos, validação de formato **e dígitos verificadores** via `CnpjUtil` mod-11 + anotação customizada `@Cnpj`)
- [x] Repository (+ busca por nome `ContainingIgnoreCase`)
- [x] Service (CRUD + normalização de máscara + bloqueio de duplicado)
- [x] DTOs
- [x] Controller CRUD: `/api/fornecedores` com filtro `?nome=`
- [x] Testes (6 unitários: normalização, DV inválido, duplicado, atualização, not found)

### Fase 6 — Licitações 📋
- [x] Entidade `Licitacao` (modalidade enum c/ modalidades das leis 8.666/14.133, status enum ABERTA→ENCERRADA→HOMOLOGADA+CANCELADA, valorEstimado, vencedor FK Fornecedor, numeroEdital único, campo objeto)
- [x] Repository + Service + DTOs (filtro por dotação via Specification: `?status=&modalidade=`)
- [x] Controller CRUD: `/api/licitacoes` (inclui endpoint para definir vencedor `PUT /{id}/vencedor` que encerra a licitação)
- [x] Regras: nasce ABERTA; datas coerentes; editável/removível só enquanto ABERTA sem vencedor; vencedor trocável até homologação
- [x] Testes (8 unitários)

### Fase 7 — Contratos 📜
- [x] Entidade `Contrato` (FKs: dotacao obrigatória, licitacao, fornecedor; saldoRestante = valorTotal no cadastro)
- [x] Método `calcularValorMensal()` (valorTotal / duracaoMeses — BigDecimal, escala 2 e HALF_UP)
- [x] Repository + Service + DTOs
- [x] Controller CRUD: `/api/contratos` (filtro combinado `?dotacaoId=&fornecedorId=&status=`)
- [x] Validações: duração >= 1, valorTotal > 0, numero único; licitação vinculada precisa estar ENCERRADA/HOMOLOGADA com o próprio fornecedor como vencedor; valorTotal/duração/vínculos imutáveis após criação
- [x] Testes (8 unitários, incluindo cálculo do valor mensal)

### Fase 8 — Módulo de Empenhos (CORE DO PROJETO) 🎯
- [x] Entidade `Empenho` (mesReferencia, anoReferencia, valor, status enum EMPENHADO/LIQUIDADO/PAGO/ANULADO + dataEmissao, FK contrato, FK usuario nullable até a Fase 9; unicidade controlada pela aplicação — ANULADO não bloqueia recriação)
- [x] `EmpenhoService.gerarEmpenho()` com `@Transactional`:
  - [x] Calcular valorMensal do contrato
  - [x] Impedir empenho duplicado (mesmo contrato + mesmo mês/ano, excluindo ANULADO) → 409
  - [x] Validar sequencialidade (mês anterior deve estar empenhado; exceto primeiro mês da vigência)
  - [x] Validar `dotacao.saldoAtual >= valorMensal` → 400 c/ mensagem detalhada
  - [x] Validar `contrato.saldoRestante >= valorMensal` → 400
  - [x] Criar empenho + debitar dotação (via `DotacaoService.debitar`, gera auditoria) + debitar contrato (tudo atômico)
  - [x] Extra: contrato deve estar VIGENTE e competência dentro da vigência (YearMonth unificado) → 409
- [x] DTOs `EmpenhoRequestDTO` (contratoId, mes, ano) e `EmpenhoResponseDTO` (rico: nº contrato, código dotação, fornecedor)
- [x] `EmpenhoController`: `POST /api/empenhos` + listagens combinadas (`?contratoId=&dotacaoId=&mes=&ano=&dataDe=&dataAte=`) + anulação `DELETE /{id}` com estorno dos dois saldos (só se EMPENHADO; registra movimentação tipo ESTORNO novo no enum TipoMovimentacao)
- [x] Exceções de negócio específicas (SaldoInsuficienteException etc.) → HTTP 400
- [x] **Testes obrigatórios**: cenário feliz, saldo insuficiente dotação, saldo insuficiente contrato, duplicidade (+10: arredondamento ÷3, fora da vigência, mês inválido, não vigente, estorno da anulação, bloqueio de anulação de liquidado, pular mês, primeiro mês sem antecessor, recriar após anulação)
- [x] Cenários Gherkin salvos em `docs/cenarios.feature` (9 cenários pt-BR)

⚠️ Nota técnica desta fase: Hibernate 6 cria CHECK constraint para colunas de enum STRING — ao adicionar o valor ESTORNO em `TipoMovimentacao` o banco rejeitava. Corrigido com `@JdbcTypeCode(SqlTypes.VARCHAR)` no campo `tipo` + `ALTER TABLE ... DROP CONSTRAINT movimentacoes_dotacao_tipo_check` (one-time). Demais enums não crescem depois de criados, então mantêm seus constraints válidos.

### Fase 9 — Segurança JWT 🔐
- [x] Dependências jjwt (api/impl/jackson 0.12.x) no pom.xml
- [x] `SecurityConfig` rewrite: stateless, CSRF off, BCryptPasswordEncoder mantido, CORS via propriedade `cors.allowed-origins`, entry point 401 + access denied 403 com JSON UTF-8 no formato de erro da API
- [x] `security/JwtService` — API jjwt 0.12 (`signWith`/`verifyWith`), claims subject=email + perfil, expiração 24h
- [x] `AuthService`: login `POST /api/auth/login` → retorna JWT + dados do usuário; registro `POST /api/auth/register` restrito ao ADMIN; BadCredentials → 401 no GlobalExceptionHandler
- [x] Filtro `JwtAuthenticationFilter` na cadeia: extrai Bearer, valida, carrega UserDetails por email e popula o SecurityContext
- [x] Regras definidas c/ usuário: ADMIN gerencia tudo e registra contas; USUARIO só lê os módulos mas gera E anula empenhos
- [x] CORS liberado via `application.properties` (`cors.allowed-origins`, default `localhost:5173,localhost:3000`)
- [x] Empenho agora nasce auditável: `EmpenhoService` resolve o usuário autenticado via SecurityContext (`usuarioId` preenchido)
- [x] Testes de integração de auth (7 cenários, RestTemplate sem error-handler + @LocalServerPort)
- [ ] Frontend consumirá `POST /api/auth/login` e enviará `Authorization: Bearer <token>`

### Fase 10 — Frontend React ⚛️ *(em 3 ondas: A scaffold/login · B CRUDs · C EmpenhoForm)*
- [x] Scaffold Vite React-TS na pasta `frontend/` (onda A)
- [x] Axios configurado (`services/api.ts`) com interceptor JWT + 401 → logout (onda A)
- [x] Roteamento (react-router-dom) + tela Login c/ RotaProtegida (onda A)
- [x] `Navbar` + layout base + Dashboard (saldos por dotação + empenhos recentes) (onda A)
- [x] Formatação de moeda/datas pt-BR (`utils/format.ts`) (onda A)
- [x] Proxy dev Vite → localhost:8080 (onda A)
- [x] Páginas CRUD: Dotações, Fornecedores, Licitações (c/ definir vencedor), Contratos (componente `TabelaGenerica` reutilizável) — **onda B**
- [x] Formulário de Empenho (`EmpenhoForm`): seleciona contrato vigente + mês/ano, feedback visual sucesso/erro mostrando saldos atualizados — **onda C** ✅ *Fase 10 concluída*

### Fase 11 — Testes, Docs e Qualidade ✅
- [x] Cobertura de testes dos services principais (mínimo: EmpenhoService, DotacaoService) — 71 testes verdes; JaCoCo 0.8.13: geral 78%, DotacaoService 95%, EmpenhoService 80%
- [x] Diagramas em `docs/diagramas/` (`.mermaid`): modelo-dados, fluxo-empenho, arquitetura-aws
- [x] README.md final (visão geral, instruções, endpoints, regras de negócio) — único .md commitado; seção de prints com TODO p/ Enzo
- [x] Revisão de endpoints com coleção de testes (`scripts/test-api.sh`: 22 verificações curl, idempotente, matriz de papéis incluída)

### Fase 12a — DevOps: Docker + CI ✅
- [x] `Dockerfile` backend multi-stage (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`)
- [x] `.dockerignore` (raiz) + `frontend/.dockerignore`
- [x] `frontend/Dockerfile` (`node:22-alpine` build → `nginx:alpine`) + `frontend/nginx.conf` (SPA fallback, proxy `/api` → `api:8080`, gzip, X-Forwarded-*)
- [x] `docker-compose.prod.yml`: db (postgres:15-alpine, healthcheck, volume `pgdata_prod`) + api (**sem porta pública**, config 100% por env vars obrigatórias `${VAR:?}`) + web (:80); senhas fora do git
- [x] `.env.example`
- [x] Workflow GitHub Actions `.github/workflows/ci.yml`: backend (service container Postgres 15 + temurin 21 + `mvnw verify` + artifact JaCoCo), frontend (lint + build) e publish na main (imagens `ghcr.io/<owner>/gestao-compras-{api,web}` tags latest+SHA via GITHUB_TOKEN)
- [x] **Validação local**: build das 2 imagens OK → compose up (db healthy → api ~13s) → smoke test **22/22 verdes** via nginx (`BASE=http://localhost`) → SPA fallback e assets OK → `down -v`

### Fase 12b — Deploy em nuvem ☁️ *(PENDENTE — decidir destino quando o projeto estiver 100%)*
**Decisão de 2026-08-24: AWS cortada.** As contas criadas no curso da faculdade foram desvinculadas e não há acesso ao billing; no regime novo do Free Tier (contas pós-15/07/2025) a janela grátis é de 6 meses com créditos de até US$200 — provavelmente já expirada. Custo do pivô é mínimo: as imagens já são publicadas no GHCR pelo CI, então qualquer destino só precisa de `pull` + `up` + smoke.

Opções pesquisadas em ago/2026:

| Opção | Oferece | Contrapartidas |
|---|---|---|
| Oracle Cloud Always Free | ARM Ampere A1 **2 OCPU/12 GB** (limite reduzido em jun/2026), 200 GB disco, 10 TB egress — **grátis pra sempre** | Cadastro burocrático c/ cartão (hold reversível); "out of capacity" em regiões populares; região fixa pra sempre; instância ociosa 30+ dias pode ser recolhida |
| GitHub Student Pack → DigitalOcean US$200 | ≈2–3 anos de droplet US$6/mês | Ativar em `education.github.com` **enquanto matriculado** (benefício expira ao formar) |
| Render + Neon | Web service free (512 MB, dorme após 15 min, acorda em ~1 min) + Postgres Neon free **permanente** (suspende após 5 min, dados preservados) + front estático grátis (Vercel/Render) | Menos aprendizado de infra; cold starts; exigiria liberar origem no CORS |
| ~~GCP e2-micro~~ | VM sempre-grátis 1 GB | Evitar: só 1 GB de egress/mês grátis |
| ~~Postgres free do Render~~ | — | Evitar: expira em 30 dias e apaga os dados |

Quando o destino for escolhido: escrever `deploy.yml` (SSH ou integração nativa da plataforma) + guia de provisionamento passo-a-passo nesta seção.

---

## 5. COMANDOS ÚTEIS

```bash
# Entrar no projeto
cd ~/Documents/Projects/gestao-compras-publicas-api

# Rodar backend
./mvnw spring-boot:run

# Compilar/testar
./mvnw clean compile
./mvnw test

# Banco de dados (após Fase 1)
docker compose up -d
docker compose down

# Frontend (após Fase 10)
cd frontend && npm install && npm run dev   # http://localhost:3000

# Frontend — lint, testes e build
cd frontend && npm run lint && npm test && npm run build

# Monitoração (actuator)
curl http://localhost:8080/actuator/health   # público
```

---

## 6. PENDÊNCIA DE AMBIENTE: INSTALAÇÃO DO DOCKER (executar manualmente)

O agente NÃO tem permissão sudo interativa. O usuário deve colar isto num terminal próprio:

```bash
# Docker CE — repositório oficial
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Rodar docker sem sudo
sudo usermod -aG docker $USER
newgrp docker

# Validar
docker run hello-world
```

⚠️ Se o codename do Ubuntu não for suportado pelo repo oficial, substituir `$(. /etc/os-release && echo "$VERSION_CODENAME")` por uma distro-base compatível (ex.: `noble`) ou instalar via `sudo apt install docker.io docker-compose-v2`.

---

## 7. PLANO DE COMMITS (execução do USUÁRIO — o agente não comita)

**Convenção**: Conventional Commits em inglês, verbo no imperativo ("add", nunca "added"), sem ponto final, tudo minúsculo após o tipo.
- 1 commit = 1 unidade coesa. NUNCA misturar módulos/fases diferentes.
- Só commitar com a fase concluída (checkboxes marcados) e `./mvnw test` passando.
- NUNCA commitar senhas ou `.env` real (só `.env.example`). Corpo de 1–2 linhas quando o "porquê" não for óbvio.
- **Fluxo por sessão**: concluir fase → marcar checkboxes → executar commit(s) da tabela → `git push`.

### Pendências atuais do git (executar AGORA, nesta ordem)

> Contexto: as 3 mudanças anteriores acabaram num único commit (`chore: remove generated HELP.md`) e o `.idea/.gitignore` está rastreado desde um commit antigo. Os comandos abaixo desfazem o último commit e o refazem corretamente em 2, além de tirar o `.idea` do repositório.

```bash
# 1. Desfaz o último commit (mantém as mudanças no diretório)
git reset HEAD~1

# 2. Commit correto nº 1
git add .gitignore .mvn/
git commit -m "chore: add gitignore and Maven wrapper config"

# 3. Commit correto nº 2 (remove HELP.md do repo + tira .idea do rastreamento)
git rm -r --cached .idea
git add HELP.md
git commit -m "chore: remove generated files from repository"

# 4. Force push seguro
git push --force-with-lease
```

### Commits da Fase 1 (após a correção acima)

```bash
git add docker-compose.yml
git commit -m "feat: add PostgreSQL via docker compose"

git add src/main/resources/application.properties
git commit -m "feat: configure datasource and JPA for local development"

git push
```

### Commits por fase

| Fase | Quando | Mensagem de commit |
|---|---|---|
| — | Agora (correção histórico) | `chore: add gitignore and Maven wrapper config` |
| — | Agora (correção histórico) | `chore: remove generated files from repository` |
| 1 | Após docker-compose.yml criado | `feat: add PostgreSQL via docker compose` |
| 1 | Após application.properties configurado e app conectando | `feat: configure datasource and JPA for local development` |
| 2 | Entidade Usuario + erro global + seed | `feat: add user entity with roles, repository and global error handling` |
| 3 | Módulo Dotações completo c/ testes | `feat: add budget allocation module with CRUD and balance endpoints` |
| 4 | Créditos suplementares completos c/ testes | `feat: add supplementary credit transfers between allocations` |
| 5 | Fornecedores completo c/ testes | `feat: add supplier module with CNPJ validation` |
| 6 | Licitações completa c/ testes | `feat: add bidding module with winner assignment` |
| 7 | Contratos completo c/ testes | `feat: add contract module with monthly value calculation` |
| 8 | EmpenhoService + Controller funcionando | `feat: implement monthly commitment generation with balance validation` |
| 8 | Após os 4 testes obrigatórios | `test: cover empenho generation scenarios` |
| 9 | JWT completo c/ testes de integração | `feat: add JWT authentication and role-based authorization` |
| 10 | Scaffold Vite + login + rotas | `feat(ui): scaffold React app with login and routing` |
| 10 | Páginas CRUD prontas | `feat(ui): add CRUD pages for core modules` |
| 10 | EmpenhoForm com feedback de saldos | `feat(ui): add empenho form with balance feedback` |
| 11 | Cobertura mínima atingida | `test: raise service layer coverage` |
| 11 | Diagramas + cenários Gherkin | `docs: add architecture diagrams and acceptance scenarios` |
| 12 | Dockerfiles prontos | `build: add multi-stage Dockerfiles` |
| 12 | Workflow Actions + AWS provisionado | `ci: add GitHub Actions pipeline with ECR and EC2 deploy` |
| 12 | README final revisado | `docs: finalize README with setup and usage guide` |

**Regras de postura profissional**
- Commits ao fim da fase, nunca código pela metade ou "WIP".
- Mensagem descreve O QUE mudou; corpo (opcional) explica POR QUÊ.
- Antes de cada push, conferir `git status` — nada de arquivos temporários, `.env`, logs.
- Commits descrevem O QUE mudou no código; decisões técnicas e contexto de cada sessão ficam no Log de Sessões.

### Programa de melhorias (8 fases) — 2026-09-07

1 commit por fase, todos já executados pelo usuário **exceto a Fase 8** (persiste `M docs/guia-de-desenvolvimento.md` + os arquivos abaixo até o fechamento).

```bash
# Fase 1 — normalizar erros 4xx/navbar
git add <...>
git commit -m "fix: normalize 4xx errors, unify error shape and fix navbar active state"

# Fase 2 — Flyway
git commit -m "feat: add flyway-managed schema with baseline and indexes"

# Fase 3 — locking financeiro
git commit -m "fix: enforce financial integrity with optimistic and pessimistic locking"

# Fase 4 — eager-fetch + retorno de entidade
git commit -m "perf: eager-fetch paginated lists and drop redundant dotacao re-fetch"

# Fase 5 — /auth/me + endurecimento prod
git commit -m "feat: expose /auth/me, validate sessions on boot and harden production auth"

# Fase 6 — frontend
git commit -m "refactor(ui): crud hook, toasts, strict TS and client-side validation"

# Fase 7 — testes
git commit -m "test: cover global exception handler and shared validation"

# Fase 8 (PENDENTE) — Actuator + docs de backup/Trivy
git add pom.xml src/main/java/com/gestaocompras/config/SecurityConfig.java src/main/resources/application.properties src/main/resources/application-prod.properties src/test/java/com/gestaocompras/integration/ActuatorSecurityTest.java docs/backup.md
git commit -m "ops: expose health via actuator, document backup/restore and add trivy guidelines"

# Fechamento — log deste programa no guia
git add docs/guia-de-desenvolvimento.md
git commit -m "docs: record improvement program and phase commits in dev guide"

git push
```

Histórico resultante (do mais antigo ao mais novo): `c157271` (F1), `42b9fc4` (F2), `d3fad77` (F3), `ee1d7b2` (F4), `9e680bb` (F5), `8188e8a` (F6), `d16f2c0` (F7), mais Fase 8 + fechamento.

### Programa multitenancy (5 fases) — 2026-09-07

1 commit por fase, executados pelo usuário. **Fase 1 implementada e validada; aguardando o usuário commitá-la.**

```bash
# Fase 1 — dados + segurança (multitenancy)
git add src/main/resources/db/migration/V4__organizacoes_multitenancy.sql \
        src/main/java/com/gestaocompras \
        src/test/java/com/gestaocompras \
        README.md docs/guia-de-desenvolvimento.md docs/diagramas/modelo-dados.mermaid
git commit -m "feat: add organization-level multitenancy (groups, members, X-Org-Id isolation)"

# Fases 2–5 (PENDENTES — comandos serão publicados ao concluir cada fase):
# F2  API de grupos/membros/convites + register público  ->  "feat: expose orgs/members/invites API and public signup"
# F3  Frontend onboarding + seletor de grupo            ->  "feat(ui): landing, signup, onboarding and org switcher"
# F4  Gestão de membros + painel SUPER_ADMIN           ->  "feat(ui): member management and super admin panel"
# F5  Fechamento (docs, diagramas, smoke script)         ->  "docs: finalize multitenancy docs and scripts"

git push
```

---

## 8. LOG DE SESSÕES

| Data | Agente/Modelo | O que foi feito |
|---|---|---|
| 2026-08-23 | ox-alpha (opencode) | Análise do prompt; diagnóstico do ambiente; restaurado `.mvn/wrapper/maven-wrapper.properties` (mvnw voltou a funcionar); validado `./mvnw -version` (Maven 3.9.16) e `./mvnw compile` (OK); criado `.gitignore` completo (com ocultação de rastros de IA); criado este PASSO-A-PASSO.md; pendente: instalação manual do Docker pelo usuário; duplicata `~/Downloads/gestao-compras-publicas` marcada para remoção após confirmação |
| 2026-08-23 | ox-alpha (opencode) | Validado Docker CE 29.7.2 + Compose v5.5.0 instalados pelo usuário (`docker ps` e `hello-world` OK sem sudo) — **Fase 0 concluída**; revisados PASSO-A-PASSO e `.gitignore`; confirmada remoção da duplicata em `~/Downloads`; decisões: commits em inglês (Conventional Commits), manter os 3 commits antigos, pom.xml intocado; criada seção **7. Plano de Commits**; pendência imediata: usuário executar os 2 commits iniciais da seção 7; próximo: Fase 1 (docker-compose.yml + application.properties) |
| 2026-08-23 | ox-alpha (opencode) | **Fase 1 implementada e validada**: criado `docker-compose.yml` (postgres:15-alpine, volume `pgdata`, healthcheck `pg_isready`, db=`gestao_compras`) e `application.properties` (datasource, `ddl-auto=update`, `open-in-view=false`, logs SQL); banco healthy; app iniciou com HikariPool conectado ao PostgreSQL em 3.6s; app encerrada, container deixado rodando p/ dev. Detectado no git: usuário juntou as pendências num único commit `c2437ec` (mensagem incompleta) e `.idea/.gitignore` está rastreado (commit antigo `0390bd3`); decisão: reescrever último commit em 2 corretos + des-rastrear `.idea` (force-push, repo novo). Decisão: frontend usará npm. Pendências do usuário: comandos de correção do histórico (seção 7) + 2 commits da Fase 1 |
| 2026-08-23 | ox-alpha (opencode) | Usuário executou correção do histórico e commits da Fase 1 (verificado no remoto: histórico conventional commits limpo; ressalva: `git add HELP.md` não registrou deleção pois o arquivo existe no disco — correção via `git rm --cached` + amend pendente na Parte A desta fase); **Fase 2 implementada e validada**: `model/Perfil` (enum), `model/Usuario` (`@Enumerated(STRING)`, email único), `repository/UsuarioRepository`, `dto/ErroResposta` (record), `exception/NotFoundException` + `GlobalExceptionHandler` (400/404/409/500), `config/SecurityConfig` temporário (permitAll + BCryptPasswordEncoder bean) e `config/DataInitializer` (seed admin@admin.com/admin idempotente); validado: compile OK, `./mvnw test` OK (contexto conecta no Postgres), app subiu em 4.2s com seed confirmado e linha no banco (`perfil=ADMIN`, hash `$2a$10$...`) |
| 2026-08-23 | ox-alpha (opencode) | Verificado remoto: HELP.md fora do git ✓, .idea fora ✓, histórico limpo (amend caiu no commit f0e4789 do datasource — mantido por decisão); **Fase 3 implementada e validada**: entidades `DotacaoOrcamentaria` (debitar/creditar com validação de saldo) + `MovimentacaoDotacao` (histórico imutável) + enum `TipoMovimentacao`; repositórios; DTOs records c/ Bean Validation; exceptions `SaldoInsuficienteException`(400)/`RegistroDuplicadoException`(409)/`OperacaoNaoPermitidaException`(409) integradas ao handler global; `DotacaoService` transacional (PUT bloqueia alteração de saldos, DELETE bloqueado com histórico, criar registra saldo inicial); `DotacaoController` CRUD completo + `/saldo` + `/movimentacoes`; 9 testes unitários Mockito — **10/10 verdes**; validação runtime curl: 201 criar, 409 duplicado, saldo 200, 400 ao alterar saldoInicial, PUT ok sem mexer em saldo, movimentações ok, DELETE 204, GET pós-delete 404 |
| 2026-08-23 | ox-alpha (opencode) | **Fase 4 implementada e validada**: entidade `CreditoSuplementar` (origem+destino LAZY, valor 19,2, data), repositório c/ `JpaSpecificationExecutor` (filtro por dotação envolvida + período sem problema de null-param no PG), DTOs request/response (response traz códigos das dotações), `CreditoSuplementarService` transacional reusando `DotacaoService.debitar/creditar` (movimentações DEBITO na origem e CREDITO_SUPLEMENTAR no destino; rollback atômico), controller POST 201 + GET filtrado; 4 testes unitários — **14/14 verdes**; runtime curl: remanejamento 10.000 entre dotações confirmou saldos 40000/12000 e histórico correto dos dois lados; negativos: origem=destino 409, saldo insuficiente 400 c/ mensagem clara, dotação inexistente 404. Obs.: ids das dotações no teste real começaram em 2 (sequence persistiu após delete da validação da Fase 3 — comportamento normal do banco) |
| 2026-08-23 | ox-alpha (opencode) | **Fase 5 implementada e validada**: `CnpjUtil` (limpeza de máscara + validação por dígitos verificadores mod-11, rejeita repetidos) reutilizado pela anotação Bean Validation customizada `@Cnpj`/`CnpjValidator` e pelo service; entidade `Fornecedor` (cnpj único 14 dígitos), repository c/ busca por nome, DTOs, `FornecedorService` (CRUD, duplicado→409, atualização não rouba CNPJ de outro), controller CRUD c/ filtro `?nome=`; 6 testes — **20/20 verdes**; runtime curl: 201 mascarado salvo como dígitos, mesmo CNPJ sem máscara → 409, DV errado → 400 "cnpj: CNPJ inválido", dígitos repetidos → 400, filtro nome, PUT email, DELETE 204, pós-delete 404 |
| 2026-08-23 | ox-alpha (opencode) | **Fase 6 implementada e validada**: enums `ModalidadeLicitacao` (9 modalidades cobrindo leis 8.666 e 14.133) e `StatusLicitacao`; entidade `Licitacao` c/ numeroEdital único, objeto (adição além do diagrama original — essencial no domínio), vencedor FK opcional e método de domínio `isEditavel()`; repository Specification p/ filtros status+modalidade combinados; service: nasce ABERTA, datas coerentes, edital duplicado→409, `definirVencedor` seta ENCERRADA e bloqueia em HOMOLOGADA/CANCELADA, editar/remover só na ABERTA; controller CRUD + `PUT /{id}/vencedor`; 8 testes — **28/28 verdes**; runtime curl: 201 ABERTA, duplicado 409, datas invertidas 400, vencedor definido → ENCERRADA c/ resumo do fornecedor, editar/remover encerrada → 409, filtros ok |
| 2026-08-23 | ox-alpha (opencode) | **Fase 7 implementada e validada**: enum `StatusContrato` (VIGENTE/ENCERRADO/RESCINDIDO — nasce VIGENTE); entidade `Contrato` c/ FKs dotação+obrigatória/fornecedor+obrigatório/licitação+opcional, `saldoRestante` nascendo = valorTotal, métodos de domínio `calcularValorMensal()` (divide c/ BigDecimal escala 2 HALF_UP) e `calcularDataFimPrevista()` (derivada de dataInicio+duração, sem coluna); service: numero único→409, regra forte de integridade licitação↔fornecedor (só ENCERRADA/HOMOLOGADA e vencedora pelo próprio fornecedor→409), PUT bloqueia alteração de valorTotal/duração/vínculos (400), filtros Specification combinados dotacaoId+fornecedorId+status; response inclui valorMensal/dataFimPrevista/resumos dos vínculos; 8 testes — **36/36 verdes**; runtime curl: contrato 201 completo c/ saldoRestante=120000 e valorMensal=10000 e dataFimPrevista=2027-08-31, numero dup 409, licitação vencida por outro fornecedor 409, PUT mudando valorTotal 400, filtros ok. **Nota Fase 8**: parcelas com resto (ex.: ÷3) somam centavos a menos que o total — decidir se última parcela absorve o resto |
| 2026-08-23 | ox-alpha (opencode) | **Fase 8 implementada e validada — CORE do projeto**: entidade `Empenho` c/ unique constraint de banco (contrato+ano+mes), status EMPENHADO/LIQUIDADO/PAGO/ANULADO, usuario nullable até a Fase 9; `EmpenhoService.gerar()` transacional: valida VIGENTE + competência dentro da vigência (`YearMonth.atDay(1)` vs dataInicio..dataFimPrevista → rateio exato), duplicado→409, saldo dotação→400 "disponível R$ X, necessário R$ Y", saldoRestante contrato→400, débito atômico reusando `DotacaoService.debitar()` (auditoria automática); anulação `DELETE` estorna os dois saldos c/ novo tipo `ESTORNO` no enum TipoMovimentacao (overload `creditar(…, tipo)` em DotacaoService, CREDITO_SUPLEMENTAR segue default p/ suplementações); controller c/ filtros combinados contrato/dotação/mês/ano/período; **bug real encontrado e corrigido**: Hibernate 6 gera CHECK constraint p/ enums STRING — valor ESTORNO era rejeitado pelo banco ("violates check constraint movimentacoes_dotacao_tipo_check"); fix: `@JdbcTypeCode(SqlTypes.VARCHAR)` + DROP CONSTRAINT one-time via psql; 10 testes (incluindo ÷3=3333.33 fluindo pro empenho e verify que debitar NEM É CHAMADO nos fluxos de erro) — **46/46 verdes**; runtime: laço jan–jun num contrato 60000/6 com dotação de 25000 → jan/fev 201, mar–jun 400 esgotamento progressivo, dup 409, jul 409 fora da vigência, anulação estornou tudo (contrato 50000, dotação 15000), auditoria completa CREDITO→DEBITO×2→CREDITO_SUPLEMENTAR(histórico pré-fix)→DEBITO→ESTORNO; docs/cenarios.feature c/ 9 cenários Gherkin pt-BR |
| 2026-08-24 | ox-alpha (opencode) | **Fase 9 implementada e validada**: jjwt 0.12.6 (api/impl/jackson) no pom — única dependência nova do projeto; `JwtService` c/ API moderna (HS384, secret Base64 de 64 bytes no properties, expiração 24h, claims email+perfil); `JwtAuthenticationFilter` stateless valida Bearer e popula SecurityContext via UserDetails carregado por email; `SecurityConfig` rewrite completo: matriz ADMIN(tudo+register)/USUARIO(leitura+gera e anula empenhos)/anônimo(só login), CORS localhost:3000, entry point e access denied devolvendo JSON UTF-8 no padrão ErroResposta; AuthService/Controller (login público → token+perfil; register só ADMIN; BadCredentials→401 novo handler global); **ciclo fechado**: EmpenhoService resolve usuário autenticado pelo email do contexto → todo empenho nasce c/ usuarioId; **descobertas Boot 4**: Jackson 3 usa coordenadas tools.jackson (import ajustado), TestRestTemplate migrou p/ spring-boot-resttestclient e sua autoconfig está QUEBRADA na 4.1.1 (OnBeanCondition falha ao deduzir tipo) — solução: RestTemplate puro + @LocalServerPort + ResponseErrorHandler no-op p/ asserções de status; teste de segurança real confirmou rejeição de assinatura adulterada e payload forjado (anexar char no fim é leniência inofensiva do decoder base64 do jjwt — bits extras caem no grupo incompleto); 7 testes de integração autocontidos (criam dotação/fornecedor/licitação/vencedor/contrato próprios c/ CNPJ gerado programaticamente) — **53/53 verdes**; runtime: login admin→token, sem token 401 JSON UTF-8 ok, adulterado 401, joão(USUARIO): escrita 403, leitura 200, register 403, empenho abr/2026 201 c/ usuarioId=5 |
| 2026-08-24 | ox-alpha (opencode) | **Fase 12a implementada e validada**: `Dockerfile` backend multi-stage (go-offline cache → package -DskipTests → JRE alpine), `.dockerignore` ×2, `frontend/Dockerfile` + `nginx.conf` (SPA fallback, proxy `/api`→api:8080, gzip), `docker-compose.prod.yml` (db healthcheck, api sem porta pública, config via env obrigatórias `${VAR:?}`, `MaxRAMPercentage=60` p/ host de 1GB), `.env.example`; workflow CI em 3 jobs (backend c/ service Postgres 15 + JaCoCo artifact, frontend lint+build, publish GHCR na main com owner lowercased e tags latest+SHA); **validação local completa**: imagens buildadas, compose up (db healthy, API 401 após ~13s), smoke 22/22 verdes via nginx, SPA fallback/assets OK, down -v; **decisão de arquitetura de deploy**: AWS cortada (contas do curso desvinculadas, billing inacessível, janela free tier provavelmente expirada) — Fase 12b adiada até projeto 100%, opções pesquisadas documentadas na seção da fase; pendências do usuário: 2 commits (`build:` e `ci:`) + push + verificar Student Pack |
| 2026-08-24 | ox-alpha (opencode) | **CI verde na 1ª run no remoto** (commits a40369e `build:` e e3201f5 `ci:` conferidos via API do GitHub: jobs backend/frontend/publish todos success em ~2m48s; imagens publicadas em ghcr.io/enzo-barbosa/gestao-compras-{api,web}, tags latest+SHA, privadas por padrão). **B4 implementado — nota da Fase 8 resolvida**: rateio por posição com novo método `Contrato.calcularValorCompetencia(YearMonth)` — parcelas intermediárias = valor mensal (HALF_UP), última competência absorve o resíduo (`total − mensal×(n−1)`) → soma das parcelas fecha exata no valorTotal independente da ordem de criação dos empenhos; anulação imune (estorna o valor gravado); EmpenhoService.gerar usa o novo método nos checks de saldo/save/débito/decremento; +5 testes (absorção na última competência, soma fecha criando mar→jan→fev, duração 1 mês = valor total, competência fora da vigência rejeitada) — **68/68 verdes**; README ganhou a regra 5 nas "Regras de negócio"; pendência do usuário: commit `feat(empenho): absorb rounding remainder into final installment` |
| 2026-08-24 | ox-alpha (opencode) | **A1 concluído**: badge de CI no topo do README (liberado após run #2/d2dc1c6 confirmada success via API); roadmap atualizado à realidade da decisão AWS — `[x] Fase 12a` / `[ ] Fase 12b` c/ candidatos (Oracle Always Free, DO via Student Pack, Render+Neon); contagens do README corrigidas com números medidos localmente: 63→**68 testes** e "~78%"→**78%** JaCoCo (instruções; ramos 60%) via `./mvnw verify` com Postgres de dev — BUILD SUCCESS 68/68. **B5 concluído — 0 warnings no oxlint**: diagnóstico correto dos "6 warnings" = 5× `set-state-in-effect` nas páginas + 1× `only-export-components` no AuthContext (não era o que constava na fila). Descoberta-chave: o oxlint NÃO respeita fronteira de `await` ao analisar funções externas chamadas do effect (qualquer setState no corpo marca warning) — validado empiricamente que passam: (a) função declarada DENTRO do effect (padrão DashboardPage) ou (b) loader promise-chain c/ setStates dentro de `.then/.catch/.finally`; escolhido (b) nas 5 páginas (Empenhos/Dotações/Fornecedores/Licitações/Contratos): `carregar` virou função que retorna a chain, handlers que recarregam agora fazem `setCarregando(true)` explícito antes do `await carregar()` (comportamento de spinner preservado); AuthContext dividido em 3 arquivos canônicos Fast-Refresh-safe (`AuthContext.ts` tipos+createContext, `AuthProvider.tsx`, `useAuth.ts`) c/ imports atualizados em 8 arquivos. Verificação final: `oxlint src/` = **0 warnings**, `npm run lint` exit 0, `npm run build` ok (tsc+vite), backend intocado. Pendências do usuário: commits sugeridos `docs:` (README) + `refactor(ui):` (frontend inteiro) e push; A2 bloqueado aguardando screenshots em docs/img/; lembretes: Student Pack + packages GHCR públicos |
| 2026-08-26 | opencode/big-pickle | **Sessão de debug — 4 fixes**: (1) **CORS fix**: `SecurityConfig` hardcoded `localhost:3000` mas Vite roda na 5173 — preflight OPTIONS retornava 403; fix: propriedade `cors.allowed-origins` no `application.properties` (default `localhost:5173,localhost:3000`) lida via `@Value` no `SecurityConfig`. (2) **Bug 3 — anulado bloqueia recriação**: `@UniqueConstraint` incondicional em `Empenho` + query `existsBy...SemFiltroStatus` tratavam ANULADO como duplicata; fix: removido `@UniqueConstraint`, nova query `existsBy...AndStatusIn` com lista de status ativos (EMPENHADO/LIQUIDADO/PAGO). (3) **Bug 2 — saldo não esgota**: `validarCompetenciaNaVigencia` usava `LocalDate` (dia exato) mas `calcularValorCompetencia` usava `YearMonth` — quando `dataInicio` não é dia 1, interseção perdia 1 mês; fix: unificado para `YearMonth` em ambos os lados. (4) **Bug 1 — pular mês**: nenhuma validação de sequencialidade existia; fix: nova `validarSequencialidade` que exige empenho ativo no mês anterior (exceto primeiro mês da vigência). (5) **.gitignore limpo**: seção "OCULTAÇÃO DE RASTROS DE IA" removida, regras úteis mantidas (`**/*.md`, `!README.md`). Testes: 71/71 (68 antigos + 3 novos: `naoDevePularMes`, `primeiroMesDaVigenciaNaoExigeCompetenciaAnterior`, `devePermitirRecriarEmpenhoAposAnulacao`). Commits: `fix(cors): externalize allowed-origins via application.properties` + `fix(empenho): resolve 3 competencia logic bugs`. Pendências: A2 (screenshots), Fase 12b (deploy). |
| 2026-08-28 | opencode/big-pickle | **Decisão de transparência (a pedido do usuário)**: removidas as regras de ocultação de uso de IA — `gitignore` deixa de bloquear `*.md`, instrução de "nunca mencionar ferramentas de IA em commits/PRs/README" substituída por registro neutro de decisões no Log de Sessões; guia movido para `docs/guia-de-desenvolvimento.md` e versionado no repositório; README ganhou link para o guia. |
| 2026-08-31 | opencode/big-pickle | **Sessão de aprimoramento pré-deploy — Fase A (guards de exclusão + page size)**. (1) `ContratoService.remover()` agora bloqueia exclusão de contrato que possua empenhos ativos (EMPENHADO/LIQUIDADO/PAGO) via nova query `EmpenhoRepository.existsByContratoIdAndStatusIn`. (2) `FornecedorService.remover()` agora bloqueia exclusão de fornecedor com contrato VIGENTE (`ContratoRepository.existsByFornecedorIdAndStatusIn`) ou que seja vencedor de licitação (`LicitacaoRepository.existsByVencedorId`). (3) `LicitacaoService.definirVencedor()` agora rejeita substituir vencedor já definido em licitação ENCERRADA (impede troca silenciosa). Todos lançam `OperacaoNaoPermitidaException` (HTTP 409). (4) Novo `config/WebConfig` com `PageableHandlerMethodArgumentResolver.setMaxPageSize(100)` — cap global no `size` de todos os endpoints. Testes: 71 → **77/77 verdes** (+2 Contrato, +3 Fornecedor, +1 Licitação). Pendências: Fases B (frontend a11y/CSS), C (profiles prod), D (nginx headers). |
| 2026-08-31 | opencode/big-pickle | **Sessão de aprimoramento pré-deploy — Fase B (acessibilidade + CSS + bugs frontend)**. (1) `index.html`: `lang="en"` → `lang="pt-BR"`. (2) ARIA: `Navbar.tsx` `<nav aria-label="Navegação principal">`; alerts dos 8 arquivos ganham `role="alert"` (erro) e `role="status"` (sucesso); `TabelaGenerica.tsx` ganha prop `ariaLabel` (default) aplicada nas 5 páginas. (3) `index.css`: definidas no `:root` as variáveis apontadas como indefinidas `--cinza-borda: #cbd5e1` e `--azul-600: #2563eb`. (4) `DotacoesPage.tsx`: ano do formulário agora dinâmico via `new Date().getFullYear()` (substitui "2026" hardcoded). (5) `ContratosPage.tsx`: removida non-null assertion `!` no `find` — agora checa null e aborta com mensagem de erro. Validação: `npm run lint` 0 warnings, `npm run build` OK (tsc + vite), backend intocado. Pendências: Fases C (profiles prod), D (nginx headers). |
| 2026-08-31 | opencode/big-pickle | **Sessão de aprimoramento pré-deploy — Fase C (configuração por ambiente/prod)**. (1) Criado `src/main/resources/application-prod.properties` com `spring.jpa.hibernate.ddl-auto=validate` (ditoa decisão aprovada: default continua dev-friendly com `update` + logs SQL; o profile `prod` sobrescreve o `ddl-auto`). (2) `docker-compose.prod.yml`: serviço `api` ganhou `SPRING_PROFILES_ACTIVE=prod` + `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (dupla proteção: profile + env explícita). Não há `application-dev.properties` — default já é o dev. Validação: `docker compose -f docker-compose.prod.yml config` OK (env refletidas), `./mvnw test` **77/77 verdes** (novo properties não afeta o default). Item "estratégia de profiles" da Fase 1 marcado como concluído. Pendência: Fase D (nginx security headers). |
| 2026-08-31 | opencode/big-pickle | **Sessão de aprimoramento pré-deploy — Fase D (nginx security headers) · FIM das 4 fases pré-deploy**. `frontend/nginx.conf`: adicionados `server_tokens off` e os headers `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff` e `Referrer-Policy: strict-origin-when-cross-origin` (todos com `always`). Sem CSP (evitar quebrar o React com inline styles) — decisão aprovada. Validação: `nginx -t` via container `nginx:alpine` (com `--add-host=api:127.0.0.1` para resolver o upstream do proxy) → **syntax is ok**. Projeto pronto para as 4 fases completarem; pendente: revisar itens adiados para o deploy real (JWT secret via env — já exigido no compose prod `JWT_SECRET:?`, senha admin fraca para dev, containers root). |
| 2026-09-07 | opencode/big-pickle | **Programa de 8 fases de melhoria (F1–F8) concluído, com 1 commit por fase**. F1: `GlobalExceptionHandler` reescrito (shape unificado via `ErroResposta`, 405/415/`HttpMessageNotReadable`, ordem de handlers com genéricos no fim) + navbar ativa. F2: schema migrado do Hibernate auto-DDL para **Flyway** (`V1__init` gerado por schema-export, `V2` c/ índices e unique real do empenho, baseline-on-migrate=1; `ddl-auto=validate` em todos os profiles). F3: **lock otimista** (`version` em dotacoes/contratos) + **pessimista** (`PESSIMISTIC_WRITE`/`findByIdComLock` no budget) + handler de `ObjectOptimisticLockingFailureException`→409. F4: `@EntityGraph` em `findAll(Specification,Pageable)` dos 4 repos (sem N+1 nas listagens) + `debitar/creditar` retornando `DotacaoOrcamentaria`. F5: `GET /api/auth/me`, validação de sessão no boot do front + aviso "sessão expirada", `JwtService` fail-fast em secret vazio, TTL 8h, `application-prod` com SQL=INFO. F6: TS strict (`noUncheckedIndexedAccess`), `useCrudPage<T>`, toasts (`ToastProvider`), `Pagina<T>`, `overflow-x`, validação client compartilhada. F7: `GlobalExceptionHandlerTest` (13 casos, MockMvc standalone) + Vitest (23 casos em format/validacao). F8: Actuator (`health` público, demais ADMIN, prod sem detalhes) + `docs/backup.md` (pg_dump/restore, systemd, drill, Trivy). Contagem final: backend **94/94**, frontend **23/23**, lint 0 warnings. Pendência: usuário executar commit da Fase 8 + fechamento do guia. |
| 2026-09-01 | opencode/big-pickle | **Sessão de revisão geral + 3 fases de aprimoramento pré-deploy (E, F, G)**. Fase E (segurança backend): (1) `application.properties` JWT secret agora é placeholder `${JWT_SECRET:default-dev}` — prod injeta via env `JWT_SECRET:?` do compose, eliminando segredo hardcoded no repo público; (2) `DataInitializer` seed do admin só roda quando o profile **NÃO** é `prod` (senha `admin` deixa de nascer em produção; criar admin manualmente lá); (3) `RegistroRequestDTO` perdeu o campo `perfil` — registro sempre cria `USUARIO` (impede auto-promoção a ADMIN via body; `AuthService.registrar` fixa `Perfil.USUARIO`); (4) `SecurityConfig` CORS `allowedHeaders` restrito de `*` para `Authorization, Content-Type`. Fase F (robustez backend): (1) `GlobalExceptionHandler` novo handler `ConstraintViolationException` (400 claro em vez de 500 para validação de params/query); (2) removido `hibernate.orm.jdbc.bind=TRACE` do default (logs não expõem mais valores de binds sensíveis). **Decisão no meio do caminho**: `@Size(min=8)` no login foi revertido — criaria inconsistência com o seed `admin`/`admin` (5 chars) que quebraria o login; o registro já exige min=8. Fase G (frontend): (1) 401 não faz mais hard reload (`window.location.href`); `api.ts` dispara evento `auth:expirado`, novo `SessaoExpiradaListener` (dentro do `BrowserRouter`/`AuthProvider`) chama `logout()` + `navigate('/login')` — logout consistente via React state, sem recarga de página; `App.tsx` reestruturado (`BrowserRouter` por fora do `AuthProvider`); (2) a11y `TabelaGenerica`: `<caption class="sr-only">`, `scope="col"` nos `<th>`, `role="status"` + `aria-busy` no carregando/vazio, classe `.sr-only` adicionada ao CSS; (3) `aria-label` contextual nos botões por linha (Editar/Excluir/Anular/Definir vencedor) nas 5 páginas; (4) `LoginPage` inputs com `autoComplete="email"`/`current-password`. **Descoberta técnica**: oxlint 1.79 **não suporta** `react-hooks/exhaustive-deps` (regra inexistente) — config não a inclui; padrão das páginas (`carregar()` promise-chain com setters estáveis) é correto sem stale-closure, então os `useEffect` não foram refatorados. **Validação final**: backend `./mvnw test` **77/77 verdes** (após E e F), frontend `npm run lint` 0 warnings + `npm run build` OK (Fase G). Pendência: Fase 12b (deploy em nuvem). |
| 2026-09-07 | opencode/big-pickle | **Programa multitenancy por grupos — Fase 1 (dados + segurança) implementada e validada, aguardando commit do usuário**. `V4__organizacoes_multitenancy.sql` (aplicado): tabelas `organizacoes`/`membros_organizacao` (papel por grupo ADMIN/OPERADOR/VISITANTE)/`convites_organizacao` (e-mail ou código); coluna `organizacao_id` nas 6 entidades de negócio + `creditos_suplementares` (nullable na migration, backfill em prod, `SET NOT NULL` runtime não-prod); uniques globais migradas para compostas por org (ordem importa: DROP CONSTRAINT IF EXISTS → recriação); perfil `SUPER_ADMIN|USUARIO`. Backend: 6 serviços org-escopados (`Long organizacaoId` 1º param), controllers repassam `organizacaoId()` do `@AuthenticationPrincipal UsuarioLogado`, repos org-escopados (`findByIdAndOrganizacaoId`, `existsByXAndOrganizacaoId`, specs), creates via `getReferenceById`. Segurança: `X-Org-Id` escolhe a organização ativa, `ROLE_<papel>` por membria (fora: 403 "Você não é membro desta organização."), `X-Org-Id` inválido 400, leitura ADMIN/OPERADOR/VISITANTE/SUPER_ADMIN, escrita ADMIN/OPERADOR/SUPER_ADMIN, `MovimentacaoDotacao` sem org, unique parcial de empenho mantida, `DataInitializer` com admin global SUPER_ADMIN + ADMIN da org "Minha Organização" (id 1). Testes: 6 serviços reescritos (69 unitários) + `AuthIntegrationTest` reescrito + novo `IsolamentoOrganizacaoIntegrationTest` → **96/96 verdes**, BUILD SUCCESS. Docs da F1 atualizadas (README, guia, `modelo-dados.mermaid`). Pendência do usuário: commit da Fase 1 (seção 7. Plano de Commits → Programa multitenancy). Próximo: Fase 2 (API de grupos/membros/convites + `register` público). |
