#!/usr/bin/env bash
# ============================================================================
# Smoke test da API — Gestão de Compras Públicas
#
# Exercita o fluxo completo: login JWT -> dotação -> fornecedor -> licitação
# -> vencedor -> contrato -> empenhos por competência -> anulação -> saldos.
#
# Uso:      ./scripts/test-api.sh            (API em http://localhost:8080)
#           BASE=http://host:porta ./scripts/test-api.sh
#
# Re-rodável infinitamente: cada execução usa sufixo único nos registros.
# A limpeza final respeita as regras de negócio (empenhos anulados e
# licitações encerradas permanecem como histórico — comportamento intencional
# do sistema). Os IDs criados são listados ao final para inspeção manual.
# Requisitos: bash, curl, python3
# ============================================================================
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
SUFIXO="$(date +%s)"
ANO="$(date +%Y)"
MES="$(date +%-m)"
MES_SEGUINTE="$(date -d '+1 month' +%-m)"
ARQ_RESPOSTA="$(mktemp)"
PASSOS=0
FALHAS=0
TOKEN=""
IDS_CRIADOS=()

verbo() { echo "── $*"; }

assert_status() {
    local descricao="$1" esperado="$2" obtido="$3"
    if [[ "$obtido" == "$esperado" ]]; then
        PASSOS=$((PASSOS + 1))
        echo "  ✓ $descricao (HTTP $obtido)"
        return 0
    fi
    FALHAS=$((FALHAS + 1))
    echo "  ✗ $descricao — esperado HTTP $esperado, obtido HTTP $obtido"
    cat "$ARQ_RESPOSTA" | head -c 300; echo
    return 1
}

requisicao() {
    local metodo="$1" caminho="$2" corpo="${3:-}" autorizacao="${4:-}"
    local args=(-s -X "$metodo" "$BASE$caminho"
        -o "$ARQ_RESPOSTA"
        -w '%{http_code}'
        -H 'Content-Type: application/json')
    [[ -n "$autorizacao" ]] && args+=(-H "Authorization: Bearer $autorizacao")
    [[ -n "$corpo" ]] && args+=(-d "$corpo")
    curl "${args[@]}"
}

campo_json() {
    python3 -c "import sys, json; dados = json.load(sys.stdin); print(dados$1)" \
        < "$ARQ_RESPOSTA" 2>/dev/null || echo ""
}

gerar_cnpj_valido() {
    calcular_dv() {
        local numero="$1"
        shift
        local -a pesos=("$@")
        local soma=0 i
        for ((i = 0; i < ${#numero}; i++)); do
            soma=$(( soma + ${numero:i:1} * pesos[i] ))
        done
        local resto=$(( soma % 11 ))
        (( resto < 2 )) && echo 0 || echo $(( 11 - resto ))
    }
    local base
    base="$(printf '%08d%04d' $(( SUFIXO % 100000000 )) 1)"
    local dv1 dv2
    dv1="$(calcular_dv "$base" 5 4 3 2 9 8 7 6 5 4 3 2)"
    dv2="$(calcular_dv "$base$dv1" 6 5 4 3 2 9 8 7 6 5 4 3 2)"
    echo "$base$dv1$dv2"
}

echo "════════════════════════════════════════════════════════════"
echo " Smoke test — Gestão de Compras Públicas ($BASE)"
echo " Sufixo único desta execução: $SUFIXO"
echo "════════════════════════════════════════════════════════════"

# ── 1. Autenticação ─────────────────────────────────────────────────────────
verbo "Autenticação"
codigo="$(requisicao GET /api/dotacoes)"
assert_status "endpoint protegido nega acesso sem token" 401 "$codigo" || true

codigo="$(requisicao POST /api/auth/login '{"email":"admin@admin.com","senha":"admin"}')"
assert_status "login do administrador" 200 "$codigo" || true
TOKEN="$(campo_json "['token']")"
if [[ -z "$TOKEN" ]]; then
    echo "✗ Sem token o teste não pode continuar."; exit 1
fi

codigo="$(requisicao POST /api/auth/login '{"email":"admin@admin.com","senha":"errada"}')"
assert_status "senha inválida é rejeitada" 401 "$codigo" || true

# ── 2. Dotação orçamentária ────────────────────────────────────────────────
verbo "Dotação orçamentária"
CORPO="{\"codigo\":\"TESTE.$SUFIXO\",\"descricao\":\"Smoke test script\",\"saldoInicial\":50000,\"anoExercicio\":$ANO}"
codigo="$(requisicao POST /api/dotacoes "$CORPO" "$TOKEN")"
assert_status "criação da dotação (saldo inicial = saldo atual)" 201 "$codigo" || true
DOTACAO_ID="$(campo_json "['id']")"
IDS_CRIADOS+=("dotação #$DOTACAO_ID")

codigo="$(requisicao POST /api/dotacoes "$CORPO" "$TOKEN")"
assert_status "código de dotação duplicado é rejeitado" 409 "$codigo" || true

# ── 3. Fornecedor ───────────────────────────────────────────────────────────
verbo "Fornecedor"
CNPJ="$(gerar_cnpj_valido)"
CORPO="{\"nome\":\"Fornecedor Smoke $SUFIXO\",\"cnpj\":\"$CNPJ\",\"email\":\"smoke$SUFIXO@teste.com\"}"
codigo="$(requisicao POST /api/fornecedores "$CORPO" "$TOKEN")"
assert_status "criação do fornecedor (CNPJ válido gerado: $CNPJ)" 201 "$codigo" || true
FORNECEDOR_ID="$(campo_json "['id']")"
IDS_CRIADOS+=("fornecedor #$FORNECEDOR_ID")

CORPO_INVALIDO='{"nome":"Inexistente","cnpj":"11111111111111"}'
codigo="$(requisicao POST /api/fornecedores "$CORPO_INVALIDO" "$TOKEN")"
assert_status "CNPJ inválido é rejeitado" 400 "$codigo" || true

# ── 4. Licitação e vencedor ─────────────────────────────────────────────────
verbo "Licitação"
HOJE="$(date +%F)"
CORPO="{\"numeroEdital\":\"SMOKE-$SUFIXO\",\"modalidade\":\"PREGAO\",\"objeto\":\"Objeto do smoke test\",\"dataAbertura\":\"$HOJE\",\"valorEstimado\":30000}"
codigo="$(requisicao POST /api/licitacoes "$CORPO" "$TOKEN")"
assert_status "criação da licitação (status ABERTA)" 201 "$codigo" || true
LICITACAO_ID="$(campo_json "['id']")"

codigo="$(requisicao PUT "/api/licitacoes/$LICITACAO_ID/vencedor" "{\"fornecedorId\":$FORNECEDOR_ID}" "$TOKEN")"
assert_status "definição do vencedor encerra a licitação" 200 "$codigo" || true
STATUS_LICITACAO="$(campo_json "['status']")"
[[ "$STATUS_LICITACAO" == "ENCERRADA" ]] \
    && assert_status "status final da licitação é ENCERRADA" ENCERRADA ENCERRADA \
    || assert_status "status final da licitação é ENCERRADA" ENCERRADA "$STATUS_LICITACAO"
IDS_CRIADOS+=("licitação #$LICITACAO_ID")

# ── 5. Contrato ─────────────────────────────────────────────────────────────
verbo "Contrato"
INICIO="$(date -d "$(date +%Y-%m)-01" +%F)"
CORPO="{\"numero\":\"SMOKE-$SUFIXO\",\"objeto\":\"Contrato do smoke test\",\"valorTotal\":30000,\"duracaoMeses\":3,\"dataInicio\":\"$INICIO\",\"dotacaoId\":$DOTACAO_ID,\"fornecedorId\":$FORNECEDOR_ID,\"licitacaoId\":$LICITACAO_ID}"
codigo="$(requisicao POST /api/contratos "$CORPO" "$TOKEN")"
assert_status "criação do contrato vinculado à tríade dotação+fornecedor+licitação" 201 "$codigo" || true
CONTRATO_ID="$(campo_json "['id']")"
VALOR_MENSAL="$(campo_json "['valorMensal']")"
IDS_CRIADOS+=("contrato #$CONTRATO_ID")
echo "  · valor mensal calculado pelo sistema: R$ $VALOR_MENSAL"

# ── 6. Empenhos por competência ─────────────────────────────────────────────
verbo "Empenhos"
CORPO="{\"contratoId\":$CONTRATO_ID,\"mesReferencia\":$MES,\"anoReferencia\":$ANO}"
codigo="$(requisicao POST /api/empenhos "$CORPO" "$TOKEN")"
assert_status "empenho da competência $(printf '%02d' "$MES")/$ANO" 201 "$codigo" || true
EMPENHO_1="$(campo_json "['id']")"
VALOR_EMPENHO="$(campo_json "['valor']")"
USUARIO_EMPENHO="$(campo_json "['usuarioId']")"
echo "  · valor empenhado: R$ $VALOR_EMPENHO (usuarioId auditor: $USUARIO_EMPENHO)"

CORPO_SEGUNDO="{\"contratoId\":$CONTRATO_ID,\"mesReferencia\":$MES_SEGUINTE,\"anoReferencia\":$ANO}"
codigo="$(requisicao POST /api/empenhos "$CORPO_SEGUNDO" "$TOKEN")"
assert_status "rateio mensal permite competência seguinte" 201 "$codigo" || true
EMPENHO_2="$(campo_json "['id']")"

codigo="$(requisicao POST /api/empenhos "$CORPO" "$TOKEN")"
assert_status "competência duplicada é rejeitada" 409 "$codigo" || true

# ── 7. Anulação e estorno ───────────────────────────────────────────────────
verbo "Anulação"
codigo="$(requisicao DELETE "/api/empenhos/$EMPENHO_1" "" "$TOKEN")"
assert_status "anulação do primeiro empenho" 200 "$codigo" || true
STATUS_ANULADO="$(campo_json "['status']")"
[[ "$STATUS_ANULADO" == "ANULADO" ]] \
    && assert_status "status pós-anulação é ANULADO" ANULADO ANULADO \
    || assert_status "status pós-anulação é ANULADO" ANULADO "$STATUS_ANULADO"

codigo="$(requisicao DELETE "/api/empenhos/$EMPENHO_1" "" "$TOKEN")"
assert_status "anulação dupla é rejeitada" 409 "$codigo" || true

codigo="$(requisicao GET "/api/dotacoes/$DOTACAO_ID/saldo" "" "$TOKEN")"
assert_status "consulta de saldo da dotação" 200 "$codigo" || true
SALDO_FINAL="$(cat "$ARQ_RESPOSTA")"
ESPERADO_SALDO="50000.00"
if python3 -c "
import sys
saldo, empenhado = float('$SALDO_FINAL'), float('$VALOR_EMPENHO')
sys.exit(0 if abs(saldo - (50000 - empenhado)) < 0.01 else 1)" 2>/dev/null; then
    PASSOS=$((PASSOS + 1))
    echo "  ✓ saldo da dotação reflete o rateio líquido: R$ $SALDO_FINAL (um empenho ativo, um anulado)"
else
    FALHAS=$((FALHAS + 1))
    echo "  ✗ saldo da dotação inesperado: R$ $SALDO_FINAL"
fi

# ── 8. Matriz de papéis ─────────────────────────────────────────────────────
verbo "Matriz de papéis"
CORPO_USUARIO="{\"nome\":\"Usuario Smoke\",\"email\":\"smoke.usuario.$SUFIXO@teste.com\",\"senha\":\"senhaSegura123\",\"perfil\":\"USUARIO\"}"
codigo="$(requisicao POST /api/auth/register "$CORPO_USUARIO")"
if [[ "$codigo" == "401" || "$codigo" == "403" ]]; then
    assert_status "registro de usuário exige ADMIN (anônimo rejeitado)" "$codigo" "$codigo"
else
    assert_status "registro de usuário exige ADMIN (anônimo rejeitado)" 401 "$codigo"
fi

codigo="$(requisicao POST /api/auth/register "$CORPO_USUARIO" "$TOKEN")"
assert_status "administrador registra usuário comum" 201 "$codigo" || true

requisicao POST /api/auth/login "{\"email\":\"smoke.usuario.$SUFIXO@teste.com\",\"senha\":\"senhaSegura123\"}" > /dev/null
TOKEN_USUARIO="$(campo_json "['token']")"

codigo="$(requisicao POST /api/fornecedores '{"nome":"Proibido","cnpj":"11444777000161"}' "$TOKEN_USUARIO")"
assert_status "usuário comum não cria fornecedor (ADMIN-only)" 403 "$codigo" || true

# ── Encerramento ────────────────────────────────────────────────────────────
rm -f "$ARQ_RESPOSTA"
echo "════════════════════════════════════════════════════════════"
echo " Resultado: $PASSOS verificações OK, $FALHAS falha(s)"
echo " Registros criados nesta execução (histórico permanente):"
for item in "${IDS_CRIADOS[@]}"; do echo "   · $item"; done
echo "════════════════════════════════════════════════════════════"
exit $(( FALHAS > 0 ? 1 : 0 ))
