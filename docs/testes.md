# Estratégia e documentação de testes

> Camada de qualidade do projeto: 144 testes backend (JUnit 5 + Mockito + Spring Boot Test), 34 testes de frontend (Vitest), smoke E2E com 45 verificações e relatório de cobertura JaCoCo.

## Pirâmide de testes

O projeto segue a pirâmide clássica: **muitos testes unitários** (rápidos e isolados), **alguns testes de integração** (fluxos reais com banco de verdade) e **um smoke E2E** (o sistema de ponta a ponta via API).

```
        /e2e\        scripts/test-api.sh — 45 verificações curl
       /integração\  Spring Boot Test + PostgreSQL real — 22 testes
      /__unitários__\  JUnit 5 + Mockito — 121 testes (services + handler global)
```

### 1. Testes unitários — `src/test/java/com/gestaocompras/service` e `.../exception`

**Tecnologia**: JUnit 5 + Mockito, sem Spring context.

**Cobrem**:
- Regras de negócio de cada módulo: `DotacaoServiceTest`, `ContratoServiceTest`, `EmpenhoServiceTest`, `LicitacaoServiceTest`, `FornecedorServiceTest`, `OrganizacaoServiceTest`, `CreditoSuplementarServiceTest`, `ConviteServiceTest`, `AdminServiceTest`.
- Rateio do contrato (parcelas com HALF_UP e a **última competência absorvendo o resíduo**), sequencialidade de competências, unicidade, vigência, saldo insuficiente (dotação e contrato), anulação com estorno completo.
- Validações cross-cutting e o envelope de erro: `GlobalExceptionHandlerTest` (13 casos, MockMvc standalone).

**Não cobrem**: interação com o banco (Hibernate/PostgreSQL — delegado aos testes de integração) e a cadeia do Spring Security.

### 2. Testes de integração — `src/test/java/com/gestaocompras/integration`

**Tecnologia**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` com **PostgreSQL real** (docker compose), chamadas HTTP via `RestTemplate` (error handler no-op para assertar status) e autenticação real (JWT).

**Cobrem** (22 testes em 5 classes + o teste de contexto):
- `AuthIntegrationTest` (8): login, token adulterado, registro, autorização por papel e o fluxo completo dotação → fornecedor → licitação → vencedor → contrato → empenho.
- `IsolamentoOrganizacaoIntegrationTest` (2): multitenancy — `X-Org-Id` define a organização de contexto e isola dados entre grupos.
- `OrganizacoesIntegrationTest` (8): ciclo de grupos, membros e convites.
- `ActuatorSecurityTest` (3): endpoints do Actuator públicos vs. protegidos, incluindo o profile `prod`.
- `AnulacaoConcorrenteIntegrationTest` (1): **race condition** — ver detalhe abaixo.
- `GestaoComprasPublicasApplicationTests` (1): assert de que o contexto da aplicação sobe.

#### O caso de concorrência (regressão)

`AnulacaoConcorrenteIntegrationTest` dispara duas anulações do mesmo empenho **simultaneamente** (2 threads, `CountDownLatch` de barreira) e apita se mais de uma for aceita — exatamente o bug real encontrado em auditoria: a anulação lia o empenho sem lock e duas requisições concorrentes podiam **estornar a dotação e o contrato duas vezes**. O fix trava o empenho com `PESSIMISTIC_WRITE` **antes** do check de status (`EmpenhoRepository.findByIdComLock`), garantindo 1× `200 OK` + 1× `409` e estorno único. O cenário correspondente está em [`docs/cenarios.feature`](cenarios.feature).

### 3. Smoke E2E — `scripts/test-api.sh`

45 verificações end-to-end via `curl` contra a API rodando: fluxo de negócio completo (dotação → fornecedor → licitação → contrato → empenhos → anulação → saldos), ciclo multitenancy (cadastro público, grupos, membros, convites por código/e-mail, papéis por grupo e isolamento por `X-Org-Id`), painel do super admin e caminhos negativos (401/400/403/409).

### 4. Frontend — `frontend/src`

- **Vitest** (34 testes) para utilidades e serviços (validação de CNPJ/CPF, formatação e clientes de API).
- **oxlint** com 0 warnings e **`tsc -b && vite build`** para tipo seguro e build limpo (executados no CI).

## Como rodar

```bash
# Backend — requer PostgreSQL de pé
docker compose up -d
./mvnw test                              # 144 testes
./mvnw verify                            # testes + relatório JaCoCo em target/site/jacoco/
./mvnw -Dtest=AnulacaoConcorrenteIntegrationTest test   # só o teste de concorrência

# Smoke E2E
./scripts/test-api.sh                    # 45 verificações

# Frontend
cd frontend && npm install
npm run lint                             # oxlint, 0 warnings
npm test                                 # 34 testes Vitest
npm run build                            # tsc + vite
```

## Cobertura (JaCoCo)

Medida em `./mvnw verify` (JaCoCo 0.8.13, 2026-09-10):

| Métrica | Cobertura |
|---|---|
| Instruções | 83,9% |
| Ramos (branches) | 69,6% |

Relatório interativo gerado em `target/site/jacoco/` (abrir `index.html`). O CI publica o relatório como artefato em cada run.