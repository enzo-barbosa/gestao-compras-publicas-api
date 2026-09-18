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

Deploy em nuvem:
- [x] **Fase 12b concluída** — Render (API) + Neon (PostgreSQL) + Vercel (front); ver runbook em [`deploy.md`](deploy.md)

### Melhorias concluídas (programa de 8 fases — sessão 2026-09-07)

- [x] Schema gerenciado por **Flyway** (migrations `V1__init`, `V2__add_indexes_and_fix_empenho_unique`, `V3__add_version_columns`, `V4__organizacoes_multitenancy`, `V5__drop_legacy_global_uniques`) com `baseline-on-migrate`; `ddl-auto=validate` em todos os profiles
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

### Programa multitenancy por grupos (5 fases) — Fase 2 (API de grupos/membros/convites + register público) implementada

- [x] **API de grupos** (`/api/organizacoes`): qualquer autenticado cria um grupo (`POST`, sem `X-Org-Id` — ainda não há membrosia) e **quem cria vira `ADMIN`**; `GET` lista os grupos do usuário com o papel em cada um; `GET /{id}` exige ser membro (senão `403` "Você não é membro desta organização."); `PUT /{id}` renomeia (ADMIN). `GET /api/auth/me` agora inclui `organizacoes` (id/nome/papel) — usado pelo onboarding do front na F3.
- [x] **API de membros** (`/api/organizacoes/{id}/membros`): listar (qualquer membro), adicionar por e-mail existente e **alterar papel** e **remover** (ADMIN). Proteções: **o criador do grupo não pode ser rebaixado nem removido** e **ninguém pode alterar/remover a própria participação** (evita auto-trancamento); sempre sobra ao menos um ADMIN (o criador).
- [x] **API de convites** (`/api/organizacoes/{id}/convites` + `/api/convites`): criar convite por **e-mail** (usuário precisa ter conta) **ou por código** (compartilhável), sempre com papel; listar pendentes e revogar (ADMIN; convite já usado não revoga). Aceite: `POST /api/convites/aceitar {codigo}` (autenticado, vira membro com o papel do convite) e `POST /api/convites/aceitar-email` (aceita todos os convites pendentes do e-mail do usuário). Convites pendentes duplicados → 409; e-mail já membro → 409; exactamente um dos campos (e-mail XOR código) é obrigatório (400).
- [x] **`POST /api/auth/register` voltou a ser público** (decidido no programa: cadastro aberto na F2, onboarding na F3); `SecurityConfig` ganhou regras específicas para `/api/organizacoes` (criar/listar autenticados; mutações e convites exigem `X-Org-Id` → `ROLE_ADMIN`) e `/api/convites/**` (autenticado); préflight CORS liberado para o header `X-Org-Id`.
- [x] **Testes**: `OrganizacaoServiceTest` (15) + `ConviteServiceTest` (17) unitários; `OrganizacoesIntegrationTest` (8) end-to-end (register público, criar grupo→ADMIN, `me` com orgs, não-membro 403, adicionar membro, criador intocável 409, aceite por código e por e-mail, OPERADOR não gerencia) + `AuthIntegrationTest` ajustado (registro agora é público) → **136/136** verdes, BUILD SUCCESS.
- Pendência da Fase 2: commit do usuário. Fase 3 (frontend: landing, cadastro, onboarding, seletor de grupo + dashboard) é a próxima.

### Programa multitenancy por grupos (5 fases) — Fase 3 (frontend: landing, cadastro, onboarding e seletor de grupo) implementada

- [x] **Landing pública em `/`**: vitrine com CTAs "Entrar"/"Criar conta"; o app protegido mudou da raiz para `/app/*` (`LayoutApp` com navbar + rotas aninhadas); + `/login`, `/cadastro`, `/onboarding`, `/selecionar-grupo` como rotas próprias.
- [x] **Cadastro aberto** (`POST /api/auth/register` via `/cadastro`): nome/e-mail/senha (mín. 8), redireciona ao login com aviso de sucesso; `X-Org-Id` sai automaticamente do `localStorage` via interceptor do axios (chave `gc_org`).
- [x] **Onboarding (`/onboarding`)**: sem nenhum grupo, o guarda `GuardiaOrganizacao` redireciona para criar grupo (POST `/api/organizacoes` — quem cria vira ADMIN), aceitar convite por código (POST `/api/convites/aceitar`) ou verificar convites do e-mail (POST `/api/convites/aceitar-email`); sempre recarrega `/auth/me` e ativa o grupo novo.
- [x] **Seletor de grupo (`/selecionar-grupo`)**: com vários grupos e sem grupo ativo, lista as organizações para escolher (ou auto-seleciona quando há só um); a navbar ganhou `<select>` de grupo ativo com papel (trocar → vai a `/app`); menções de permissão passam a considerar o **papel na organização** (`ADMIN`/`OPERADOR`/`VISITANTE`) além do perfil global.
- [x] **Dashboard e páginas de negócio** seguem usando o header `X-Org-Id` do grupo ativo — mesma segurança da F1/F2, agora dirigida pela interface.
- [x] **Testes**: novos unitários do front (Vitest) para `organizacoes.ts` (`organizacaoAtiva`, `destinoPosLogin` com auto-seleção de grupo único, `podeGerir`/`podeEmitirEmpenho`) → frontend 34/34; `oxlint` limpo e `tsc -b && vite build` OK.
- Pendência da Fase 3: commit do usuário. Fase 4 (frontend: gestão de membros/convites e painel oculto de super admin) é a próxima.

### Programa multitenancy por grupos (5 fases) — Fase 4 (gestão de membros/convites + painel SUPER_ADMIN) implementada

- [x] **Backend mínimo de apoio** (novidade pontual para alimentar o painel): `AdminService` + `AdminController` em `/api/admin/**` restrito a `SUPER_ADMIN` (regra adicionada **antes** do genérico `/api/**` no `SecurityConfig`): `GET /api/admin/usuarios`, `PUT /api/admin/usuarios/{id}/perfil` e `GET /api/admin/organizacoes` (com total de membros). Proteções: **não pode rebaixar o próprio perfil** e **não pode rebaixar o último SUPER_ADMIN do sistema** (ambos `409`); `UsuarioRepository.countByPerfil` novo. 7 testes unitários → backend **143/143** verdes.
- [x] **`/app/membros` (`MembrosPage`)** com abas **Membros** e **Convites**: membros — tabela (nome/e-mail/papel `select`/desde), adicionar por e-mail com papel, trocar papel, remover (proteções do criador/auto-ações vêm do backend 409); convites — criar por **e-mail ou código** com papel (envia só um dos campos), listar pendentes e revogar; erros no envelope padrão.
- [x] **`/app/superpainel` (`SuperPainelPage`)** oculto: link na navbar apenas para perfil `SUPER_ADMIN`; rota redireciona para `/app` para os demais. Abas **Usuários** (trocar perfil com `select`) e **Organizações** (id/nome/criado em/total de membros).
- [x] **Navbar**: novos links "Membros" (quando `ADMIN` no grupo ou `SUPER_ADMIN`) e "Super admin" (somente `SUPER_ADMIN`); rotas registradas no `LayoutApp`.
- [x] **Testes/qualidade**: `oxlint` limpo (padrão de loading alinhado ao `EmpenhosPage`), `tsc -b && vite build` OK, Vitest 34/34 mantidos.
- Pendência da Fase 4: commit do usuário. Fase 5 (fechamento: revisão de docs, diagramas e `scripts/test-api.sh`) é a próxima e última.

### Programa multitenancy por grupos (5 fases) — Fase 5 (Fechamento: docs, diagramas e smoke script) implementada

- [x] **`scripts/test-api.sh` reescrito para o programa multitenancy** (45 verificações): agora **45 passos positivos/negativos** cobrindo o fluxo de negócio completo com o header `X-Org-Id` (dotação → fornecedor → licitação → vencedor → contrato → empenhos → anulação → saldos) e o ciclo multitenancy: cadastro público sem token, criação de grupo (criador vira ADMIN), membros por e-mail e papel, convites por código/e-mail com aceite, matriz de papéis por grupo (VISITANTE lê e não escreve; OPERADOR cria fornecedor), isolamento (não-membro com `X-Org-Id` de outro grupo → 403; membro sem header → 403) e painel do super admin (`/api/admin/**`). A organização do negócio é resolvida dinamicamente via `GET /api/auth/me` do seed (não mais chumbada em `1`). Registros/comparativos antigos fora do novo contrato foram removidos (ex.: matriz de papéis pré-multitenancy, register que exigia ADMIN). Mantidas: sufixos únicos por execução (re-rodável), `assert_status` em qualquer HTTP, CNPJ válido gerado em runtime, caminhos negativos 401/400/403/409 e exit code 1 na presença de falhas.
- [x] **Bug real de multitenancy encontrado e corrigido pela F5**: em bancos criados antes do Flyway (baseline), as uniques globais de V1 foram criadas pelo Hibernate `ddl-auto` com **nomes gerados** (`uk4waq31...` em `dotacoes.codigo`, `uktflo0rfxy...` em `fornecedores.cnpj`, `ukm6kysis...` em `licitacoes.numero_edital`, `uk9d9mvis...` em `contratos.numero`) — e os `DROP CONSTRAINT IF EXISTS uk_*` com nomes hardcoded da V4 **não os encontraram**, deixando as uniques globais ativas e quebrando o multitenancy (CNPJ/código/edital/número únicos entre TODOS os grupos — reprodutível criando o mesmo CNPJ em dois grupos). **Fix: `V5__drop_legacy_global_uniques.sql`** derruba dinamicamente (DO block + `pg_constraint`, por coluna única e `conname NOT LIKE 'uk_%_org_%'`) qualquer única global remanescente nas 4 tabelas de negócio, preservando as compostas `uk_*_org_*` (no-op em bancos criados já via Flyway V1→V4).
- [x] **Diagramas revisados**: `docs/diagramas/modelo-dados.mermaid` conferido contra o schema da V4/V5 + API — sem divergências; `arquitetura-aws.mermaid` e `fluxo-empenho.mermaid` permanecem válidos.
- [x] **Docs atualizados**: README com a Fase 5 do roadmap marcada, descrição do smoke script (45 verificações e o que cobre), correção do texto do cadastro (register é **público**, não mais "exclusivo de administrador") e migrations `V1–V5`.
- [x] **Validação final**: smoke script executado contra a API real **45/45**; migrations aplicadas via Flyway no boot (`Successfully applied v5`).
- Programa multitenancy concluído. Programa completo: 1 commit por fase (F1–F5), todos com validação `./mvnw -o test` (143/143) + frontend (`oxlint`, Vitest 34/34, build) + smoke 45/45.

### Programa de melhorias de conta e UX (5 etapas) — implementado (sessão 2026-09-13)

- [x] **Etapa 1 (backend, `feat(auth)`)** — revogação real de sessões com versão de token: migration `V6__add_token_version.sql` (`usuarios.versao_token`), claim `vt` no JWT + checagem no filtro, `PUT /api/auth/minha-conta`, `PUT /api/auth/alterar-senha` (valida senha atual, **re-emite token** e invalida demais sessões) e `POST /api/auth/logout-todos`. Testes de integração novos. Lembrete de Lombok: `@Builder` ignora initializer → `@Builder.Default` em `versaoToken`.
- [x] **Etapa 2 (front, `feat(ui)`)**, **Etapa 3 (`feat(ui)`)**, **Etapa 4 (`feat(ui)`, landing + polish)** — componente `CampoSenha` (olho mostrar/ocultar), cadastro com "Confirmar senha", página `/app/conta` (editar nome, alterar senha, sair em todos os dispositivos) e landing redesenhada.
- [x] **Etapa 5 (`docs`)** — README e Log de Sessões atualizados.
- Pendência do usuário: commits (1 por etapa) + push → Deploy Hook Vercel → conferir bundle. Backend **152/152**, front Vitest 42/42, `oxlint` 0, build OK.

⚠️ **Testes e app exigem o Postgres rodando** (`docker compose up -d`) — o contexto Spring conecta no banco para o `ddl-auto`.

### Programa de redesign de UX/UI (2026-09-14) — Fase A (consistência visual) implementada

- [x] **Base visual**: fonte **Inter** (Google Fonts) via `<link>` + `font-family` no `body`; `<meta name="theme-color" content="#0f2a43">`; inputs/selects com `border-radius: 10px` e foco com anel suave (`box-shadow 0 0 0 3px rgba(37,99,235,.15)`); `.card` com borda (`#e7edf4`) + sombra dupla; botões com hover de elevação leve (`translateY(-1px)`).
- [x] **Correção real A1**: `.btn.secundario` era sobrescrito **globalmente** pela landing (`background: rgba(255,255,255,.14); color:#fff`) → botões **Cancelar/Editar/Verificar convites ficavam invisíveis** em todas as páginas do app (Dotações/Contratos/Membros/Onboarding). Fix: override escopado para `.tela-landing .btn.secundario`; dentro do app o padrão voltou a ser `--cinza-300`/`--texto`.
- [x] **Correção real A2**: `.btn.fantasma` (Entrar no topo) tinha `13px`/`padding 6px 12px` ↔ `.btn.primario` `14px`/`10px 16px` → desalinhado no mesmo topo. Fix: fantasma padronizado na base; versão compacta virou **exclusiva da navbar** (`.navbar .btn.fantasma`); abas/filtros ganham borda `--cinza-borda` e cor `--azul-900`.
- [x] **Topo público**: novo componente `TopoPublico` (marca + Entrar/Criar conta) compartilhado por landing, login, cadastro, esqueci-senha, onboarding e seletor; CSS `tela-login` removido.
- [x] **Login/cadastro/esqueci-senha em 2 colunas**: `.tela-split` (gradiente azul) + `PainelMarca` (eyebrow + headline + bullets `✓`) à esquerda e formulário branco (`painel-form`, radius 24px) à direita; abaixo de 880px vira 1 coluna (painel de marca por cima).
- [x] **Landing**: rodapé removido; seção "Por que o GCP" → **"Feito para quem cuida do orçamento"**; grade de recursos `minmax(300px,1fr)` (3×2) — antes `230px` deixava linhas órfãs; copy de hero/passos/cards/CTA reescrita.
- [x] **Gênero removido da UI** (decisão do usuário): fieldset no cadastro, select em Minha Conta, `generoRotulo` (função apagada), `saudacaoBemVindo` fixa "Bem-vindo(a), {nome}!", `genero` removido de `UsuarioLogado`/`AuthProvider`/normalização e de `format.test.ts`. **Backend intocado**: sem migration, campos seguem opcionais nos DTOs — o front só não envia mais.
- [x] **Dashboard**: saudação "Bem-vindo(a), {nome}!" + nome do grupo ativo (`.dica` com `orgAtiva.nome`).
- [x] **Empenhos**: coluna "Usuário" (`#id` cru) removida.
- [x] **Textos**: h2/ariaLabel `Dotações orçamentárias` → `Dotações`; placeholders no formulário de dotação (código, descrição, saldo, ano).
- Validação: `oxlint` 0, **Vitest 50/50**, `tsc -b && vite build` OK. Backend intocado. Próximas: **Fase B** (grupo pessoal automático no cadastro + página `/app/creditos` + avatar/dropdown na navbar), **Fase C** (opcionais: modal de confirmação no lugar de `window.confirm`; `usuarioNome` no DTO de empenho). Após aprovação do visual, regravar o GIF demo.

### Programa de redesign de UX/UI (2026-09-14) — Fase B (grupo pessoal automático + créditos + navbar) implementada

- [x] **Backend — grupo pessoal no cadastro**: `AuthService.registrar` passa a criar automaticamente o grupo `Espaço de {nome}` (com sufixo ` (2)`, ` (3)`… caso o nome já exista, respeitando a unicidade global por `equalsIgnoreCase`) com o novo usuário como **ADMIN** — é a primeira organização listada no `/me`/login; `AuthService` passou a depender de `OrganizacaoRepository`. Sem migration (reusa tabelas existentes). Usuários antigos (criados antes desta mudança) seguem sem grupo e caem na página **Grupos** após o login.
- [x] **Backend — testes/scripts ajustados**: `OrganizacoesIntegrationTest` passou a esperar **2** organizações no `/me` após criar um grupo (espaço pessoal + novo grupo, ambos ADMIN); `scripts/test-api.sh` valida o "espaço pessoal" (`organizacoes` = 1 e papel `ADMIN`) no lugar da checagem "começa sem grupos". Backend **172/172 verdes**, BUILD SUCCESS.
- [x] **Navbar**: links **Integrantes** e **Grupos** agora visíveis para todos; novo link **Créditos** (dentro dos módulos de negócio); botões "Minha conta"/"Sair" substituídos por **avatar com iniciais + dropdown** (cabeçalho com nome/e-mail/papel do grupo ativo; itens "Minha conta" e "Sair"); fechamento ao clicar fora (`mousedown`). Media query `≤1300px` estreita os links da navegação (medir/reaver em 1024/1280/1600).
- [x] **Onboarding → página "Grupos"**: o gate do `GuardiaOrganizacao` que redirecionava para `/onboarding` sem grupo foi removido (com o espaço pessoal o fluxo nunca trava); `OnboardingPage` foi **substituída** por `GruposPage` dentro do app (`/app/grupos`), acessível pela navbar; `/onboarding` virou redirect para `/app/grupos`; `destinoPosLogin` sem grupo aponta para `/app/grupos`.
- [x] **Página `/app/creditos`** (créditos suplementares): nova `CreditosPage` conectada ao `POST/GET /api/creditos-suplementares` — formulário com selects de origem/destino (mutuamente exclusivos), valor, data e descrição (form gated por `podeOperar`; lista visível a todos os membros, ordenada por data desc); rota `creditos` adicionada no `LayoutApp`.
- [x] **Limpeza**: CSS morto do fieldset de gênero (`.grupo-radio`/`.radio-linha`) removido.
- Validação: oxlint **0**, **Vitest 50/50**, `tsc -b && vite build` OK, backend **172/172**. Pendências na época: Fase C e regravação do GIF demo (ver seções seguintes).

### Programa de redesign de UX/UI (2026-09-14) — Fase C (modais de confirmação + `usuarioNome` no empenho) implementada

- [x] **Modal de confirmação** substituindo `window.confirm`: novo componente acessível `ModalConfirmacao` (overlay `rgba(15,42,67,.55)` com card centralizado, `role="dialog"`/`aria-modal`, foco automático no botão de confirmar, `Esc` e clique no fundo cancelam, botões desabilitados durante a operação). O `useCrudPage` passou a expor `exclusao`/`confirmarExclusao`/`cancelarExclusao`/`excluindo` (guarda o item pendente em estado em vez de bloquear o `window.confirm`) e as 4 páginas CRUD (`Dotacoes`, `Fornecedores`, `Contratos`, `Licitacoes`) renderizam o modal; `EmpenhosPage` (anular empenho), `MembrosPage` (remover integrante + revogar convite) também migradas. O padrão de "clique de novo para confirmar" de Sair do grupo foi mantido. Removidos os 7 `window.confirm` da base.
- [x] **`EmpenhoResponseDTO`**: campo `usuarioId` (id cru, sem consumo no front desde a Fase A) substituído por **`usuarioNome`** (nome legível do emissor) — muda o `record` e o mapeamento em `from()`; `EmpenhoServiceTest` ajustado (`resposta.usuarioNome()` = "João") e `AuthIntegrationTest` (/empenhos responde `usuarioNome` preenchido). Front intocado (não consome nenhum dos dois). Sem migration.
- Validação: oxlint **0**, **Vitest 50/50**, `tsc -b && vite build` OK, backend **172/172** (BUILD SUCCESS). Restante do programa: **regravação do GIF demo** após aprovação do visual (o `docs/img/demo.gif` antigo foi removido do working tree e será regravado do zero). Programa de redesign **concluído** (Fases A, B e C).

### Melhoria pós-programa (2026-09-14) — marca clicável como "home"

- [x] A marca **"GCP Compras Públicas"** no topo esquerdo virou um link de "início": no `TopoPublico` (landing, login, cadastro, esqueci-senha, seleção de grupo) leva a **`/`** (homepage); na `Navbar` (todas as páginas logadas) leva a **`/app`** (Dashboard). CSS: `.marca` agora é âncora — `color: inherit` (mantém branco no navbar `/ #fff`) + `text-decoration: none` + hover de leve opacidade (0.85). `aria-label` no link ("Página inicial" / "Ir para o dashboard"). Sem rotas novas; Guardia/redirecionamentos intactos.

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
- [x] Frontend consumirá `POST /api/auth/login` e enviará `Authorization: Bearer <token>`

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

### Fase 12b — Deploy em nuvem ☁️ *(CONCLUÍDA em 2026-09-12 — Render + Neon + Vercel)*
**Decisão de 2026-08-24: AWS cortada.** Contas do curso desvinculadas, sem acesso ao billing; Free
Tier novo (pós-15/07/2025) com janela de 6 meses provavelmente expirada. Opções pesquisadas abaixo
ficam como histórico.

**Decisão final (2026-09-12):** API Spring Boot no **Render** (Web Service free), Postgres no
**Neon** (free), front React/Vite no **Vercel** (static com SPA fallback). O provisionamento real,
as variáveis de ambiente (por nome), os fluxos de deploy/rollback e o troubleshooting estão no
**[runbook de deploy → `deploy.md`](deploy.md)**. Descobertas operacionais relevantes:
- Deploy automático via integração GitHub do Vercel **não entregou eventos** — o canal confiável
  para subir o front é o **Deploy Hook** (seção 4 do runbook).
- Ações do deploy inicial registraram também o **bug do `ehAdmin`** corrigido no front (ver Log de
  Sessões) e a **validação E2E** do fluxo de negócio em produção via API (42/42 verificações).

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

1 commit por fase, executados pelo usuário. **Fases 1–4 commitadas e validadas** (`6644198`, `96d498a`, `f6d91e8`, `90a97e5`). Pendente: Fase 5.

```bash
# Fase 1 — dados + segurança (multitenancy)   [executada ✓ 6644198]
git add src/main/resources/db/migration/V4__organizacoes_multitenancy.sql \
        src/main/java/com/gestaocompras \
        src/test/java/com/gestaocompras \
        README.md docs/guia-de-desenvolvimento.md docs/diagramas/modelo-dados.mermaid
git commit -m "feat: add organization-level multitenancy (groups, members, X-Org-Id isolation)"

# Fase 2 — API de grupos/membros/convites + register público   [executada ✓ 96d498a]
git add src/main/java/com/gestaocompras src/test/java/com/gestaocompras \
        README.md docs/guia-de-desenvolvimento.md
git commit -m "feat: expose orgs/members/invites API and public signup"

# Fase 3 — landing, cadastro, onboarding e seletor de grupo   [executada ✓ f6d91e8]
git add frontend/src frontend/package.json frontend/package-lock.json \
        README.md docs/guia-de-desenvolvimento.md
git commit -m "feat(ui): landing, signup, onboarding and org switcher"

# Fase 4 — gestão de membros/convites + painel SUPER_ADMIN   [executada ✓ 90a97e5]
git add src/main/java/com/gestaocompras src/test/java/com/gestaocompras \
        frontend/src README.md docs/guia-de-desenvolvimento.md
git commit -m "feat(ui): member management and super admin panel"

# Fase 5 — Fechamento (docs, diagramas, smoke script)
git add scripts/test-api.sh README.md docs/guia-de-desenvolvimento.md \
        src/main/resources/db/migration/V5__drop_legacy_global_uniques.sql
git commit -m "docs: finalize multitenancy docs and scripts"

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
| 2026-09-07 | opencode/big-pickle | **Programa multitenancy — Fase 3 implementada e validada (aguardando commit)**: landing pública em `/`, app movido para `/app/*`; cadastro aberto (`/cadastro`), onboarding (`/onboarding`: criar grupo, aceitar código, verificar convites do e‑mail), seletor de grupo (`/selecionar-grupo` com auto-seleção de grupo único) e navbar com `<select>` de grupo ativo + badge de papel; interceptor axios envia `X-Org-Id` da chave `gc_org`; permissões de menu passam a considerar papel na organização (ADMIN/OPERADOR/VISITANTE) além do perfil global. Testes de front: `organizacoes.test.ts` novo → **34/34** Vitest, `oxlint` limpo, `tsc -b && vite build` OK. Pendência: commit da Fase 3. Próximo: Fase 4 (gestão de membros/convites no front + painel oculto de SUPER_ADMIN). |
| 2026-09-11 | opencode/big-pickle | **Regra "empenho só na competência corrente" (fechamento da lacuna conceitual)**. Contexto: o sistema rateava e validava vigência/sequência, mas qualquer competência da vigência podia ser empenhada — contrariava a ideia central do projeto ("só reservar a dotação na competência do desembolso"). Implementado: `EmpenhoService.validarCompetenciaEmitivel` exige competência = corrente ou imediatamente anterior pendente → senão `OperacaoNaoPermitidaException` (409) c/ mensagem clara; `Clock` injetável (novo `ClockConfig`, default system zone) e `dataEmissao = LocalDate.now(clock)`. Testes: `EmpenhoServiceTest` reancorado via `Clock.fixed("2026-03-15T10:00:00Z")` (corrente 03/2026, pendente 02/2026) + testes novos (competência futura e retroativa além de 1 mês → 409); integrações (`AuthIntegrationTest`, `AnulacaoConcorrenteIntegrationTest`) ancoradas em `YearMonth.now()`; smoke `scripts/test-api.sh` reformulado (mês anterior pendente 201 → corrente 201 → futuro 409 → duplicada 409) e frontend `EmpenhoForm` com validação client + dica da regra. Validação: backend **147/147**, smoke **46 verificações ok**, JaCoCo **84,0% instr / 69,9% ramos**, frontend 34/34 + lint 0 + build OK. Docs: README (regra 4), `cenarios.feature` (9→11 cenários, contexto em 03/2026), `testes.md`. Pendência do usuário: commits `feat:` + `docs:` e push. |
| 2026-09-07 | opencode/big-pickle | **Programa multitenancy — Fase 4 implementada e validada (aguardando commit)**: backend mínimo `/api/admin/**` (só `SUPER_ADMIN`): lista usuários, altera perfil (proíbe rebaixar-se e derrubar o último super admin) e lista organizações com nº de membros (`AdminService`/`AdminController` + `countByPerfil`) → **143/143** backend. Front: `/app/membros` com abas membros+convites (adicionar por e-mail, trocar papel, remover; convite por e-mail OU código, listar/revogar) e `/app/superpainel` oculto (link só para `SUPER_ADMIN`; rota redireciona os demais) com abas usuários (troca de perfil) e organizações; navbar ganhou "Membros" e "Super admin"; `oxlint` limpo, Vitest 34/34, `vite build` OK. Pendência: commit da Fase 4. Próximo: Fase 5 (fechamento). |
| 2026-09-07 | opencode/big-pickle | **Programa multitenancy — Fase 2 (API de grupos/membros/convites + cadastro público) implementada e validada, aguardando commit** (`96d498a`, executado depois). `/api/organizacoes`: criar (autenticado, criador vira ADMIN), listar os meus com papel, buscar, renomear (ADMIN); `/membros`: listar (membro), adicionar por e-mail existente, alterar papel/remover (ADMIN) com proteções (criador não pode ser rebaixado/removido; ninguém altera a própria participação; sobra sempre um ADMIN); `/convites`: criar por e-mail **ou** código (XOR, 400), listar pendentes/revogar (ADMIN), `aceitar {codigo}` e `aceitar-email`; `POST /api/auth/register` voltou a ser **público**; `SecurityConfig` com regras específicas de orgs/convites e CORS liberando o header `X-Org-Id`. Testes: `OrganizacaoServiceTest` (15) + `ConviteServiceTest` (17) + `OrganizacoesIntegrationTest` (8 e2e) → **136/136** verdes. |
| 2026-09-07 | opencode/big-pickle | **Programa multitenancy — Fase 5 (Fechamento) implementada e validada (aguardando commit)**: `scripts/test-api.sh` reescrito para o programa — 45 verificações end-to-end cobrindo o fluxo de negócio com `X-Org-Id` e o ciclo multitenancy (cadastro público, grupos, membros, convites por código/e-mail, papéis por grupo, isolamento, painel `/api/admin/**`); org do negócio resolvida dinamicamente via `/auth/me`; negativos 401/400/403/409. **Bug real encontrado pelo smoke e corrigido**: uniques globais legadas (nomes gerados pelo Hibernate `ddl-auto` pré-Flyway) não foram derrubadas pela V4 (que usava nomes hardcoded) — CNPJ/código/edital/número continuavam únicos entre todos os grupos; fix `V5__drop_legacy_global_uniques.sql` (DO block dinâmico por coluna única, preservando `uk_*_org_*`), aplicado no boot ("Successfully applied v5"). Smoke re-rodado → **45/45**; diagrama `modelo-dados.mermaid` conferido (OK); README/guia atualizados (roadmap F5 `[x]`, migrations V1–V5, plano de commits F1–F5). Programa multitenancy completo; pendência única: commit do usuário (seção 7). |
| 2026-09-07 | opencode/big-pickle | **A2 concluído — 10 prints renovados + bug de roteamento encontrado e corrigido**. (1) **Bug real no frontend**: as rotas filhas do `<Routes>` dentro do `LayoutApp` usavam caminhos absolutos (`path="/app/empenhos"`…) sob o pai `path="/app/*"` — inválido em React Router 7: **nenhuma página filha abria** (o fallback `*` redirecionava tudo para `/app`, sem erro no console). Documentado empiricamente via Playwright (login → SPA click → hard reload); fix: `LayoutApp` passou a usar rotas **relativas** (`index` + `path="empenhos"` etc.) — padrão correto para `<Routes>` descendente. Dashboard/Empenhos/Membros/Super painel renderizam até em hard reload em links profundos. (2) **Prints (A2)**: scripts de captura Playwright (`chrome` headless + `playwright-core`, em `/tmp`, não versionados) regeneraram os 10 screenshots de `docs/img/` cobrindo a UI multitenancy: landing, cadastro, login, onboarding, seletor de grupo, dashboard, empenhos, membros, membros-convites e super painel; dados de demonstração semeveados via API (usuários ana.nova/0 grupos ↔ onboarding, bruno.multi/2 grupos ↔ seletor, maria + convites pendentes ↔ membros). README: seção "Prints do sistema" reescrita com os 10 prints. Frontend validado: `oxlint` limpo, Vitest 34/34, `tsc -b && vite build` OK. |
| 2026-09-10 | opencode/big-pickle | **Sessão de auditoria + fix de concorrência + Swagger + documentação de testes (4 etapas, 1 commit cada)**. **Etapa 1 — bug real encontrado em auditoria**: `EmpenhoService.anular` lia o empenho **sem lock** (`buscarEntidade`) e só depois travava o contrato; duas anulações simultâneas passavam ambas no check de status (`EMPENHADO`) e a segunda **estornava dotação e saldo do contrato duas vezes**. Fix: `EmpenhoRepository.findByIdComLock(id, organizacaoId)` com `@Lock(PESSIMISTIC_WRITE)` (padrão do `DotacaoRepository`), usado como primeiro passo de `anular` — a segunda thread bloqueia no SELECT FOR UPDATE e, ao re-ler, vê `ANULADO` → `409`. Sem mudança de schema. 3 stubs de `EmpenhoServiceTest` migrados para `findByIdComLock` (+ `verify`). Commit sugerido: `fix(empenho): guard concurrent annulment with pessimistic lock on empenho`. **Etapa 2 — regressão**: `AnulacaoConcorrenteIntegrationTest` (2 threads + `CountDownLatch` de barreira, 1× `200` + 1× `409`, saldos conferidos via `/saldo` e `saldoRestante`) → 143→**144/144**. Commit: `test: add concurrent annulment integration regression test`. **Etapa 3 — Swagger/OpenAPI**: `springdoc-openapi-starter-webmvc-ui` 3.1.0 (compatível com Spring Boot 4) no pom; `config/OpenApiConfig` com scheme **Bearer JWT** + header global **`X-Org-Id`** via `OpenApiCustomizer` (exceto `/api/auth/*`); `SecurityConfig` libera `/swagger-ui/*`, `/v3/api-docs/*` **só quando `swagger.ativo=true`** (default dev); prod desabilita via `swagger.ativo=false` + `springdoc.api-docs.enabled=false` + `springdoc.swagger-ui.enabled=false`. Validado ao vivo: dev `/swagger-ui.html`→200 e `/v3/api-docs`→200 (título, bearerAuth e `X-Org-Id` na spec; ausente em `/api/auth/login`); prod → **401** nas duas rotas. Commit: `feat: expose api docs via springdoc openapi`. **Etapa 4 — documentação**: criado `docs/testes.md` (pirâmide unit 121 → integração 22 → E2E 45; JaCoCo medido 83,9% instruções / 69,6% ramos); `docs/cenarios.feature` +1 cenário → **10 cenários** (anulação concorrente); README com âncoras para testes, contagem 143→144, seção "Documentação interativa da API (Swagger/OpenAPI)" e nota de desabilitação em prod; este registro no Log de Sessões. Commit: `docs: add testing strategy, concurrency scenario and api docs references`. Decisão de deploy registrada na conversa: **Oracle Always Free** (eterno/grátis) para o projeto ir ao ar — Vercel descartada (não roda Java/Postgres); detalhes a aplicar depois em `docker-compose.prod.yml`/`docs/guia-deploy-oracle.md`. |
| 2026-09-12 | opencode/big-pickle | **Deploy em produção concluído (Fase 12b) + bug real corrigido + validação E2E 42/42**. **Deploy**: API Spring Boot no **Render** (Web Service free, Dockerfile, porta 8080, health `/actuator/health`, perfil prod ativo), Postgres no **Neon** (migrations Flyway V1–V5, conexão pooled `sslmode=require`; usuários/criação de primeiro admin em prod são manuais/manuais de cadastro), front React/Vite no **Vercel** (root `frontend`, `vercel.json` SPA fallback, `VITE_API_URL` apontando para `/api` em prod); CORS de prod via `CORS_ALLOWED_ORIGINS`. Documentação operacional completa criada em **`docs/deploy.md`** (arquitetura, env vars por nome — repo público, fluxos de deploy/rollback, checklist pós-deploy e troubleshooting). **Bug real (UI)**: `AuthProvider.ehAdmin` comparava o **perfil global** (`usuario.perfil === 'ADMIN'`) que só assume `USUARIO`/`SUPER_ADMIN` — resultado: `ehAdmin` sempre falso e **todos os formulários CRUD (dotação/fornecedor/licitação/contrato) ficavam ocultos** para qualquer papel; confirmado ao vivo (nenhuma opção "Nova dotação" em `/app/dotacoes`); fix: papel derivado da **organização ativa** (`podeGerir(perfil, orgPapel)`) com notificação de troca de org via evento `gc:org` em `organizacoes.ts`; validado (lint 0, Vitest 34/34, build OK); commit `40d3b4b`. **Deploy**: integração GitHub do Vercel **não entregou eventos de push** neste repo → criado **Deploy Hook** como canal confiável (`curl -X POST`); bundle novo no ar em ~1 min (conferido via `/assets/index-*.js` + `--compressed`). **Validação E2E em produção via API** (org descartável "E2E Auto", script `/tmp/e2e-prod.sh` não versionado): fluxo completo de negócio dotação(100k)→fornecedor(CNPJ válido)→licitação+vencedor→contrato 60k/12m→empenho 09/2026 (débito 5k)→duplicada 409→competência futura 409→anulação com estorno→re-empenho 201; multitenancy: OPERADOR lê/escreve, VISITANTE só lê, não-membro 403, `X-Org-Id` inexistente 403, não-numérico 400, ausente 403 (sem vazamento); contrato **não** debita dotação (só o empenho por competência). **42/42 verificações verdes**. Pendências do usuário: commits `docs:` (deploy.md + guia) + revisão visual no navegador + promoção do SUPER_ADMIN (SQL no Neon) + teste de regressão `papelAtivo` proposto. |
| 2026-09-12 | opencode/big-pickle | **Refactor de UI — `podeGerir`/`podeEmitirEmpenho` como funções puras + teste de regressão** (commit `88fa318`). A lógica "quem pode ver/editar" deixou de ser duplicada entre `AuthProvider`/`Navbar` e as páginas: `papeis.ts` exporta `podeGerir(perfil, papelOrg)` e `podeEmitirEmpenho(...)` (perfil global `SUPER_ADMIN`/`USUARIO` × papel por grupo `ADMIN`/`OPERADOR`/`VISITANTE`); `papeis.test.ts` novo com casos de regressão; navbar e páginas passaram a usar as funções. Frontend validado: Vitest, `oxlint` limpo, `tsc -b && vite build` OK. Commit + push do usuário executados. |
| 2026-09-13 | opencode/big-pickle | **Upgrade de UX/UI — revogação de sessão, página "Minha conta", senha com olho e landing (5 etapas)**. **Etapa 1 (backend)** — logout real em todos os dispositivos: migration `V6__add_token_version.sql` (`usuarios.versao_token int NOT NULL DEFAULT 0`); `Usuario.versaoToken` (atenção: Lombok `@Builder` ignora initializer → necessário `@Builder.Default`); `JwtService.gerarToken(email, perfil, versaoToken)` com claim `"vt"` + `extrairVersaoToken` (default 0, compat com tokens antigos); `JwtAuthenticationFilter` rejeita token cujo `vt` difere da coluna (JWT stateless continua, revogação por versão semântica); `AuthService.atualizarNome` / `alterarSenha` (senha atual errada → 400 "Senha atual incorreta"; nova == atual → 400; bump de versão + **re-emite token** mantendo a sessão atual) / `sairEmTodosDispositivos`; `AuthController`: `PUT /api/auth/minha-conta`, `PUT /api/auth/alterar-senha`, `POST /api/auth/logout-todos`; `SecurityConfig` com as faces autenticadas garantidas antes do genérico `/api/**`; DTOs `AlterarNomeRequestDTO`/`AlterarSenhaRequestDTO`. Testes: `AuthIntegrationTest` +5 (editar nome reflete no `/me`; senha atual incorreta 400; troca ok com token antigo 401 e novo 200; login antigo 401 / novo 200; `logout-todos` invalida o token velho e preserva novo login) → backend **152/152**, BUILD SUCCESS. **Etapa 2 (front)** — `CampoSenha` (input + olho SVG inline, `aria-pressed`/`aria-label`, sem dependência nova), `LoginPage` usa o componente, `CadastroPage` ganha campo **"Confirmar senha"** + validação client (mín. 8 + coincidência, mensagem inline), `validacao.ts` com `senhaValidaMinima`/`confirmaSenhaValida` (+ testes). **Etapa 3 (front)** — página `/app/conta` (`MinhaContaPage`): editar nome (`PUT /auth/minha-conta` + recarrega `/me`), alterar senha (3 `CampoSenha`; guarda o token novo na `TOKEN_KEY`; avisa que as demais sessões foram encerradas), "Sair em todos os dispositivos" (confirmação em 2 cliques → `logout-todos` → `logout()` → `/login` com aviso "Sessão encerrada em todos os dispositivos"); rota relativa no `LayoutApp` + link "Minha conta" na `Navbar`. **Etapa 4 (front)** — landing redesenhada (hero com eyebrow + CTAs, "Como funciona" em 3 passos numerados, grade de 6 recursos, CTA final em gradiente; rodapé "Israel Mota · estudo técnico" mantido) + polish do design system (`:focus-visible` global, transições de botão/card, `.btn.perigo.confirmando`, responsividade da navbar; `meta description` no `index.html`). **Etapa 5 (docs)** — README (endpoints de conta novos na tabela, counts 152/42, migrations V1–V6, roadmap 12b `[x]` + melhorias de conta `[x]`) + Log de Sessões. Frontend validado em todas as etapas: `oxlint` 0 warnings, Vitest 42/42, `tsc -b && vite build` OK. Pendência do usuário: commits (1 por etapa) + push na main → Deploy Hook Vercel → conferir bundle `/assets/index-*.js`. Extras opcionais pós-deploy: roteiro visual (Pref. no navegador) e SQL de limpeza do E2E. |
| 2026-09-13 | opencode/big-pickle | **Recuperação do deploy do front + novo fluxo cotidiano (após upgrade de UX)**. **Contexto**: usuário commitou as 5 etapas do upgrade (`feat(auth)` `21e0331` → `feat(ui)` ×3 → `docs:` `114899e`) e deu push; a API no Render subiu sozinha (token novo com claim `vt` confirmado → backend novo no ar). **Incidente no front**: domínio oficial devolvia `404 DEPLOYMENT_NOT_FOUND` (projecto sem deployment) e o Deploy Hook de produção devolvia `not_found: Cannot find a project with id prj_cdfup1XFlQOwVfaty2yLVNRmGnj` — **causa raiz: o hook foi apagado no Vercel** (o projeto em si continua vivo: `gestao-compras-publicas-api`, team `enzo-barbosas-projects`, id `prj_cdfup1XFlQOwVfaty2yLVNRmGnj` — confirmado pelo `VERCEL_OIDC_TOKEN` do `.env.local`). **Recuperação via CLI**: `npx vercel login` (device code) + `npx vercel link --yes` (linkou o projeto existente; o `vercel env add` então revelou que `VITE_API_URL` **já existia** em production — não recriada; valor correto `https://gestao-compras-publicas-api.onrender.com/api` confirmado embutido no bundle servido) + `npx vercel deploy --prod` (39s; alias `gestao-compras-publicas-api.vercel.app` → 200). **Domínio oficial restaurado**: `gestao-compras-publicas.vercel.app` re-anexado ao projeto (Vercel Settings → Domains → Add Domain → "Connect to an environment" → Production; listado como Valid Configuration nos dois domínios). **CORS no Render**: `CORS_ALLOWED_ORIGINS` atualizado para `https://gestao-compras-publicas-api.vercel.app,https://gestao-compras-publicas.vercel.app`; redeploy manual (3m14s) com Flyway `Successfully validated 6 migrations`, schema v6, profile prod. **Verificação final**: preflight CORS → 200 + `allow-origin` para ambos os domínios; `/`, `/login`, `/cadastro`, `/app`, `/app/conta` → 200 no domínio oficial; bundle `index-DY1eURwD.js` servido; `/actuator/health` UP. **Novo fluxo cotidiano (documentado em `docs/deploy.md`)**: backend = `git push origin main` (auto-deploy); front = `cd frontend && npx vercel deploy --prod` (vínculo em `frontend/.vercel`, gitignored; num clone novo, `npx vercel login && npx vercel link --yes` uma vez antes). Hook antigo continua morto — opcional recriar um novo (Settings → Git → Deploy Hooks) se quiser o `curl -X POST` de volta. `.env.local` (token OIDC, expira ~4h) ficou gitignored e não foi versionado. |
| 2026-09-14 | opencode/big-pickle | **Programa de redesign de UX/UI — Fase A (consistência visual) implementada e validada (aguardando commit do usuário) — sessão 2026-09-14, modelo big-pickle**. **Correções reais**: (1) `.btn.secundario` sobrescrito globalmente pela landing (`rgba(255,255,255,.14)`/`#fff`) deixava **Cancelar/Editar/Verificar convites invisíveis** no app → override escopado para `.tela-landing .btn.secundario`; (2) `.btn.fantasma` (13px) menor que o `.btn.primario` (14px) no topo → base padronizada e compacto reservado à navbar (`.navbar .btn.fantasma`); abas/filtros com borda `--cinza-borda`. **Base visual**: fonte Inter (Google Fonts) + `theme-color` #0f2a43, inputs `border-radius:10px` com anel de foco, `.card` com borda+sombra dupla, hover de elevação nos botões. **Topo público**: novo `TopoPublico` compartilhado (landing/login/cadastro/esqueci-senha/onboarding/seletor); `tela-login` removido. **Telas de auth em 2 colunas**: `.tela-split` + `PainelMarca` (bullets) à esquerda + formulário branco à direita; <880px 1 coluna. **Landing**: rodapé removido, "Feito para quem cuida do orçamento", grade `minmax(300px,1fr)` (3×2), copy reescrita. **Gênero removido da UI** (cadastro + Minha Conta + `AuthContext`/`AuthProvider`/`format.ts`/`format.test.ts`); saudação fixa "Bem-vindo(a), {nome}!"; dashboard mostra nome + grupo ativo; empenhos sem coluna `#id`; h2 "Dotações" + placeholders. Backend intocado. Validação: `oxlint` 0, **Vitest 50/50**, `tsc -b && vite build` OK. Pendências: Fase B (grupo pessoal automático no cadastro, `/app/creditos`, avatar/dropdown na navbar), Fase C (modal de confirmação; `usuarioNome` no empenho), regravação do GIF demo após aprovação do visual. |
| 2026-09-14 | opencode/big-pickle | **Programa de redesign de UX/UI — Fase B implementada e validada (aguardando commit do usuário)**. **Backend**: `AuthService.registrar` cria automaticamente o **espaço pessoal** "Espaço de {nome}" (sufixo ` (2)`… se o nome já existir; unicidade por `equalsIgnoreCase`) com o novo usuário como ADMIN (injeta `OrganizacaoRepository`; sem migration); `OrganizacoesIntegrationTest` ajustado (me com **2** orgs — espaço pessoal + grupo criado, ambos ADMIN); `scripts/test-api.sh` valida espaço pessoal (1 org, papel ADMIN) no lugar de "começa sem grupos". Backend **172/172 verdes**, BUILD SUCCESS. **Frontend**: navbar com link **Grupos** e **Integrantes** para todos, novo link **Créditos**, botões de conta substituídos por **avatar com dropdown** (iniciais + nome/e-mail/papel do grupo ativo + Minha conta/Sair, fecha clicando fora); `GuardiaOrganizacao` sem gate para quem não tem grupo; `OnboardingPage` **substituída** pela página opcional **Grupos** (`/app/grupos`, link na navbar; `/onboarding` virou redirect; `destinoPosLogin` sem grupo → `/app/grupos`); nova **`/app/creditos`** (`CreditosPage`) ligada a `POST/GET /api/creditos-suplementares` (origem/destino mutuamente exclusivos, valor/data/descrição; form gated por `podeOperar`, lista por data desc); media query `≤1300px` estreita links da navbar (medir 1024/1280/1600); CSS morto do gênero removido. Validação: oxlint **0**, **Vitest 50/50**, `tsc -b && vite build` OK. Pendências: Fase C (modal de confirmação; `usuarioNome` no empenho — aguardando análise/nova decisão), regravação do **GIF demo** (o `docs/img/demo.gif` antigo foi removido da working tree e será regravado do zero após aprovação do visual). Commits sugeridos e pendentes do usuário (backend/front/docs separados). |
| 2026-09-14 | opencode/big-pickle | **Programa de redesign de UX/UI — Fase C implementada e validada (aguardando commit/push do usuário)**. **Modal de confirmação**: novo componente acessível `ModalConfirmacao` (overlay com card, `role="dialog"`, foco automático, `Esc` cobre e fundo cancela, botões desabilitados em operação); `useCrudPage` agora guarda o item pendente em estado (`exclusao`/`confirmarExclusao`/`cancelarExclusao`/`excluindo`) e as 4 páginas CRUD renderizam o modal; `EmpenhosPage` (anular empenho), `MembrosPage` (remover integrante + revogar convite) migrados; os 7 `window.confirm` da base foram removidos (o padrão "clique de novo" de Sair do grupo foi mantido). **Empenho DTO**: `usuarioId` (sem consumo desde a Fase A) foi **substituído** por `usuarioNome` (nome do emissor) no `EmpenhoResponseDTO` (`record` + `from()`), com `EmpenhoServiceTest` e `AuthIntegrationTest` ajustados; front intocado. Decisão do usuário: opção "Trocar usuarioId por usuarioNome (Recomendado)" — análise com citações entregue (DTO, entidade, consumidores). Validação: oxlint **0**, **Vitest 50/50**, build OK, backend **172/172** (BUILD SUCCESS). Programas de redesign (Fases A+B+C) concluído; reste **regravação do GIF demo** (o antigo foi removido da working tree; regravar após aprovação do visual). Pendência no git: a **remoção do `OnboardingPage.tsx` não entrou no commit da Fase B** (working tree com ` D`); além disso, a Fase B (3 commits) **ainda não foi enviada** (`origin/main` em `90b565b`, branch ahead 3) — comandos de commit/push sugeridos. |
| 2026-09-14 | opencode/big-pickle | **Melhoria pós-programa — marca clicável como "home" (implementada e validada)**. A marca **"GCP Compras Públicas"** no canto superior esquerdo virou link de início: no `TopoPublico` (landing, login, cadastro, esqueci-senha, seleção de grupo) navega para **`/`** (homepage); na `Navbar` (páginas logadas) navega para **`/app`** (Dashboard). CSS: `.marca` virou âncora com `color: inherit` (preserva `#fff` herdada do `.navbar`), `text-decoration: none` e hover `opacity .85`; `aria-label` ("Página inicial" / "Ir para o dashboard"). Versão simples escolhida pelo usuário (sempre `/` no público). Sem rotas novas — Guardia/redirecionamentos intactos. Validação: oxlint **0**, **Vitest 50/50**, `tsc -b && vite build` OK. Backend intocado. Restam: regravação do GIF demo + deploy do front na Vercel (`npx vercel deploy --prod`). |
| 2026-09-14 | opencode/big-pickle | **Regravação do GIF demo (pós-redesign) + deploy do front na Vercel**. **Deploy**: `npx vercel deploy --prod` (CLI autenticada como enzo-barbosa; deploy a partir de `frontend/`) → build remoto `tsc -b && vite build` limpo, alias `gestao-compras-publicas.vercel.app` → 200 em `/`, `/login`, `/cadastro`, `/app`, `/app/empenhos` (bundle novo: `index-CM6JOm1i.js` — hash difere do local pois embute `VITE_API_URL` de produção). Já inclui a marca-home e as Fases A+B+C. **GIF demo regravado**: pipeline ad-hoc em `/tmp/demo-gif/` (não versionado; `playwright-core` 1.63 + `pngjs` + `gif-encoder-2` puro JS — `gifencoder` clássico puxa `canvas` nativo que falha sem libs de sistema; Chrome do sistema em `executablePath /usr/bin/google-chrome`), stack local: backend dev (`./mvnw -o spring-boot:run`, Postgres container Up) + front `npm run dev` (proxy `/api→8080`). **Seed via API** (org descartável, usuário `demo{sufixo}@prefeitura.gov.br`, espaço pessoal da Fase B como org): 2 dotações (100k e 50k) → fornecedor CNPJ válido `11222333000181` → licitação PREGAO + vencedor (precisa `valorEstimado`) → contrato 60k/6m iniciando 08/2026 → empenhos 08+09/2026 (regra de sequencialidade: empenhar mês N exige N-1; mês inicial do contrato isenta) → crédito suplementar 5k (B→A). **Tour capturado** (11 cenas, 1366×768, GIF 40 frames @400ms ≈ 15s, 3,02MB): landing → login → dashboard (saudação, saldos, empenhos recentes) → empenhos → **Anular abre o modal** (Fase C) → confirmar → toast de sucesso → **marca GCP volta ao dashboard** (feature nova) → dropdown do avatar → Minha conta → Créditos (lista com o item seedado) → Grupos. README: legenda do demo atualizada ("login, dashboard, anulação de empenho com confirmação e módulos como créditos e grupos"). Frames conferidos por seletores Playwright (cada cena confirmou heading/estado; dashboard pós-marca provado pelo h2 "Bem-vindo"). GIF um pouco maior que os antigos (~2,2MB) — aceitável; validação visual final fica com o usuário. Backend + front dev seguem rodando em background (pid 90100/90273) caso o usuário queira conferir no navegador. |
| 2026-09-15 | opencode/big-pickle | **Prints do sistema recapturados pós-redesign (1920×1080, JPEG q90) — substituindo os de 07/09 (pré-redesign)**. Todos os 10 prints de `docs/img` eram de antes do programa de redesign/UX; com as Fases A/B/C + marca-home a UI mudou (TopoPublico, tela-split, landing nova, avatar/dropdown, saudação com grupo, coluna `#id` removida em Empenhos). Script novo ad-hoc `/tmp/demo-capture/capture-prints.js` (não versionado; `playwright-core` 1.63 no `/tmp/demo-capture` + Chrome do sistema `/usr/bin/google-chrome`) com **seed via API**: usuário `prints{sufixo}@prefeitura.gov.br` (espaço pessoal + grupo **"Secretaria de Saúde"** como ADMIN), 2 dotações (100k/50k), fornecedor CNPJ válido `11222333000181`, licitação PREGAO + vencedor, contrato 60k/6m iniciando 08/2026, 2 empenhos 08+09/2026, 1 crédito suplementar B→A (5k), membro Ana Rosa (OPERADOR) + convite pendente (VISITANTE); superpainel capturado com `admin@admin.com` (SUPER_ADMIN, org id 1). Fluxo navegado: login real pela UI (aguarda token no `localStorage`), seletor de grupo pós-login (Guardião redireciona por não haver org ativa), escolha do grupo, depois navegação às demais páginas. **Resultado**: 11 prints re-capturados (landing, login, cadastro, selecionar-grupo, dashboard, empenhos, membros, membros-convites, superpainel) + **2 novos** (`/app/grupos` → `grupos.jpeg`, `/app/creditos` → `creditos.jpeg`); **`onboarding.jpeg` removido** (o `/onboarding` virou redirect para `/app/grupos` na Fase B — print obsoleto). README: seção "Prints do sistema" reescrita (onboarding sai, entram Grupos e Créditos). Notas: o nome do grupo é globalmente único — limpezas pontuais no banco dev (orgs `Secretaria de Saúde`/`Espaço de {prints}` e usuários `prints%`/`ana.rosa%`) necessárias a cada nova execução; `docs/img/demo.gif` (3,16MB) permanece intacto. Backend/front dev seguem rodando em background. |
| 2026-09-15 | opencode/big-pickle | **Filtros server-side nas listagens de negócio** (commit do usuário `1d75fe3` — registro retroativo, sem entrada no log na ocasião). `useCrudPage` passou a reagir à mudança de `config.params`: novo ref `prevParamsJson` + effect que compara `JSON.stringify(params)` e só recarrega quando muda (evita loop do antigo effect de sincronização do ref). Nova classe `.barra-filtros` no `index.css`. Cada listagem ganhou filtros enviados como query params: Dotações por `anoExercicio`, Fornecedores por nome, Licitações por status/modalidade, Contratos por status/fornecedor/dotação, Empenhos e Créditos com filtros combinados. Frontend validado (oxlint, Vitest, `tsc -b && vite build`). |
| 2026-09-17 | opencode/big-pickle | **Auditoria de 10 problemas apontados + correção em 4 lotes (front alta → front média → backend → docs)**, precedida de verificação item a item no código (todos confirmados; o exemplo do item 4 sobre `/app/dotacoes` era impreciso — o repro real é já estando em `/app`). **Lote 1 (front alta)**: (1) `format.ts` `extrairMensagemErro` deixou de engolir `Error` puro (agora retorna `erro.message`, preservando a mensagem custom de `montarCorpo` e das validações) e `format.test.ts` atualizado; (2) `AuthProvider.orgPapel` deixou de ser estado com corrida no boot e virou **derivado** via `useMemo([usuario, orgId])`, com `orgId` sincronizado por listener `EVENTO_ORG`; (3) `EmpenhosPage` passa a respeitar `podeOperar` (formulário e ações de anular escondidos de VISITANTE). **Lote 2 (front média)**: (4) `useCrudPage` e Dashboard recarregam ao trocar o grupo ativo (listener `EVENTO_ORG`; antes `navigate('/app')` era no-op e a lista ficava com os dados do grupo anterior); (5) validação de sessão no boot — `AuthProvider` expõe `validando` (inicializado pelo token, liberado em `finally`), `RotaProtegida` mostra "Carregando…" e `LoginPage` usa `destinoPosLogin(...)` + `definirOrgAtiva` no lugar do `navegar('/app')` hardcoded; (6) `ContratosPage` só exibe toast de sucesso depois do `await` do POST/PUT (antes mentia sucesso em caso de erro). **Lote 3 (backend)**: (7) removido `unique = true` global de `Contrato.numero`, `DotacaoOrcamentaria.codigo`, `Fornecedor.cnpj` e `Licitacao.numeroEdital` — contradizia o multitenancy, pois a unicidade correta é a composta `uk_*_org_*` da V4 (já garantida por `existsBy*AndOrganizacaoId` nos services); (8) `OrganizacaoRepository.existsByNomeIgnoreCase` substitui o full scan `findAll()` em `AuthService.nomeJaUsado` e `OrganizacaoService.validarNomeDisponivel`, e a migration **`V8__organizacoes_nome_unico.sql`** cria o índice único funcional `uk_organizacoes_nome_ci` (`lower(nome)`), fechando a corrida de nomes duplicados no banco. **Lote 4 (docs)**: README (migrations V1–V8, 172 backend / 51 front, 11 cenários Gherkin, smoke 52), `testes.md` (unit 140 / integração 32 / total 172 / Vitest 51 / E2E 52; `AuthIntegrationTest` 8→17), checkbox da Fase 9 do guia (login do front) marcado e este Log. Item 9 (rate limiting em login/registro/recuperação de senha) **registrado como hardening futuro** nesta sessão e **implementado na entrada seguinte**. Validação: backend **172/172** (0 failures/errors, com Postgres de dev), oxlint **0**, Vitest **51/51**, `tsc -b && vite build` OK. Pendência do usuário: commits (sugerido 1 por lote) e push. |
| 2026-09-17 | opencode/big-pickle | **Lote 5 — rate limiting nas rotas públicas de autenticação (item 9), implementado e validado; resíduos do Lote 4 corrigidos**. Design **sem dependência nova** (sem Bucket4j/Caffeine): janela fixa em memória com `ConcurrentHashMap` + `compute` atômico e `Clock` injetado (reusa o bean do `ClockConfig`). Novos `config/RateLimitProperties` (`@ConfigurationProperties("rate-limit")`: `habilitado`, `maximoChaves`, mapa `limites`), `service/RateLimitService` (`Decisao(permitido, retryAfter)`; poda entradas expiradas quando o mapa passa do máximo, usando a maior janela configurada) e `config/RateLimitFilter` (`@Component` + `@ConditionalOnProperty("rate-limit.habilitado", matchIfMissing=true)`, auto-registrado — roda depois da cadeia de segurança e antes do controller; `SecurityConfig` só ganhou `Retry-After` em `setExposedHeaders`). Rotas/limites (defaults, sobrescrevíveis por env): `login` 10/60s, `register` 5/300s, `esqueci-senha` 3/300s, `redefinir-senha` 10/900s; chave = rota + IP (`getRemoteAddr()`); resposta `429` + `Retry-After` com envelope `ErroResposta` (o `extrairMensagemErro` do front já lê `mensagem`, então aparece na UI). Prod: `application-prod.properties` ganhou `server.forward-headers-strategy=framework` para o IP real vir do proxy do Render. **Isolamento de testes**: `pom.xml` com `<systemPropertyVariables><RATE_LIMIT_HABILITADO>false</RATE_LIMIT_HABILITADO></...>` no surefire (senão os ~17 logins do `AuthIntegrationTest` do mesmo IP estourariam o limite). Testes novos: `RateLimitServiceTest` (6 casos, `Clock` mockado) + `RateLimitFilterTest` (2 casos, `@SpringBootTest` RANDOM_PORT com `@TestPropertySource` ligando o limiter e `login.requisicoes=2`) → backend **180/180**, BUILD SUCCESS. Docs: README (180 testes + bullet de proteção), `testes.md` (146 unit / 34 integração / 180), `deploy.md` (envs `RATE_LIMIT_*`, nota do proxy, troubleshooting `429` e seção de segurança com ressalva de múltiplas réplicas) e resíduos do Lote 4 (`deploy.md` migrations `V1–V8` + exemplos `V9__...` + smoke 52; `backup.md` `V1–V8`). Commits pendentes do usuário: `feat(security): rate limit authentication endpoints` (código) e `docs: document rate limiting and refresh counts and migrations` (docs dos Lotes 4 e 5). |
| 2026-09-17 | opencode/big-pickle | **Cleanup "Para onde ir" (front)**: extraído o helper `paramsListagem(sort?)` em `frontend/src/utils/listagem.ts` (`{ size: 100, sort? }`) e removida a constante `PARAMS_BASE` duplicada de 5 listagens (`Contratos` → `dataInicio,desc`, `Licitacoes` → `dataAbertura,desc`, `Creditos` → `data,desc`, `Fornecedores`/`Dotacoes` sem sort). A "guarda" `podeOperar` **não** foi abstraída de propósito: a lógica já está centralizada em `services/organizacoes.ts` (`podeOperar`/`podeGerir`) e extrair um `FormularioCard` só adicionaria indireção. Sem mudança de comportamento. Validação: oxlint **0**, Vitest **51/51**, `tsc -b && vite build` OK. Commit pendente do usuário: `refactor(ui): extract list params helper`. |
