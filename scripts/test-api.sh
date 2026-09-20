#!/usr/bin/env bash
# ============================================================================
# Smoke test da API — Gestão de Compras Públicas (multitenancy)
#
# Exercita o fluxo de negócio (login JWT -> dotação -> fornecedor -> licitação
# -> vencedor -> contrato -> empenhos -> anulação -> saldos) dentro de um
# grupo, o ciclo multitenancy (cadastro público -> criar grupo -> membros ->
# convites nominais por e-mail e código de acesso da organização -> papéis por
# grupo -> painel do super admin -> isolamento por X-Org-Id) e a redefinição
# de senha (pública, confirmando a senha atual, e pelo ADMIN do grupo).
#
# Uso:      ./scripts/test-api.sh            (API em http://localhost:8080)
#           BASE=http://host:porta ./scripts/test-api.sh
#
# Re-rodável infinitamente: cada execução usa sufixo único no nome dos grupos,
# e-mails e códigos de acesso. Empenhos anulados e licitações encerradas
# permanecem como histórico (comportamento intencional do sistema).
# Requisitos: bash (GNU date), curl, python3
# ============================================================================
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
SUFIXO="$(date +%s)"
ANO="$(date +%Y)"
MES="$(date +%-m)"
INICIO_MES="$(date +%Y-%m)-01"
MES_ANTERIOR="$(date -d "$INICIO_MES -1 month" +%-m)"
ANO_ANTERIOR="$(date -d "$INICIO_MES -1 month" +%Y)"
MES_SEGUINTE="$(date -d "$INICIO_MES +1 month" +%-m)"
ANO_SEGUINTE="$(date -d "$INICIO_MES +1 month" +%Y)"
ARQ_RESPOSTA="$(mktemp)"
PASSOS=0
FALHAS=0
TOKEN=""
ORG_ADMIN=""
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
    head -c 300 "$ARQ_RESPOSTA"; echo
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

requisicao_org() {
    local metodo="$1" caminho="$2" corpo="${3:-}" autorizacao="${4:-}" orgao="${5:-}"
    local args=(-s -X "$metodo" "$BASE$caminho"
        -o "$ARQ_RESPOSTA"
        -w '%{http_code}'
        -H 'Content-Type: application/json')
    [[ -n "$autorizacao" ]] && args+=(-H "Authorization: Bearer $autorizacao")
    [[ -n "$orgao" ]] && args+=(-H "X-Org-Id: $orgao")
    [[ -n "$corpo" ]] && args+=(-d "$corpo")
    curl "${args[@]}"
}

campo_json() {
    python3 -c "import sys, json; dados = json.load(sys.stdin); print(dados$1)" \
        < "$ARQ_RESPOSTA" 2>/dev/null || echo ""
}

tamanho_array() {
    python3 -c "import sys, json; print(len(json.load(sys.stdin)))" \
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
assert_status "login do super administrador (seed)" 200 "$codigo" || true
TOKEN="$(campo_json "['token']")"
if [[ -z "$TOKEN" ]]; then
    echo "✗ Sem token o teste não pode continuar."; exit 1
fi

codigo="$(requisicao POST /api/auth/login '{"email":"admin@admin.com","senha":"errada"}')"
assert_status "senha inválida é rejeitada" 401 "$codigo" || true

# O seed garante o super admin como ADMIN do grupo "Minha Organização" (id 1).
codigo="$(requisicao GET /api/auth/me "" "$TOKEN")"
assert_status "super admin consulta /auth/me" 200 "$codigo" || true
ORG_ADMIN="$(campo_json "['organizacoes'][0]['id']")"
if [[ -z "$ORG_ADMIN" ]]; then
    echo "✗ O seed não retornou nenhum grupo para o super admin."; exit 1
fi
echo "  · grupo-base para o fluxo de negócio: id $ORG_ADMIN"

# ── 2. Dotação orçamentária (escopo do grupo) ────────────────────────────────
verbo "Dotação orçamentária"
CORPO="{\"codigo\":\"TESTE.$SUFIXO\",\"descricao\":\"Smoke test script\",\"saldoInicial\":50000,\"anoExercicio\":$ANO}"
codigo="$(requisicao_org POST /api/dotacoes "$CORPO" "$TOKEN" "$ORG_ADMIN")"
assert_status "criação da dotação (saldo inicial = saldo atual)" 201 "$codigo" || true
DOTACAO_ID="$(campo_json "['id']")"
IDS_CRIADOS+=("dotação #$DOTACAO_ID (grupo $ORG_ADMIN)")

codigo="$(requisicao_org GET "/api/dotacoes/$DOTACAO_ID/movimentacoes" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "histórico de movimentações da dotação" 200 "$codigo" || true

codigo="$(requisicao_org POST /api/dotacoes "$CORPO" "$TOKEN" "$ORG_ADMIN")"
assert_status "código de dotação duplicado é rejeitado" 409 "$codigo" || true

codigo="$(requisicao_org GET /api/dotacoes "" "$TOKEN" "$ORG_ADMIN")"
assert_status "listagem das dotações do grupo" 200 "$codigo" || true

# ── 3. Fornecedor ───────────────────────────────────────────────────────────
verbo "Fornecedor"
CNPJ="$(gerar_cnpj_valido)"
CORPO="{\"nome\":\"Fornecedor Smoke $SUFIXO\",\"cnpj\":\"$CNPJ\",\"email\":\"smoke$SUFIXO@teste.com\"}"
codigo="$(requisicao_org POST /api/fornecedores "$CORPO" "$TOKEN" "$ORG_ADMIN")"
assert_status "criação do fornecedor (CNPJ válido gerado: $CNPJ)" 201 "$codigo" || true
FORNECEDOR_ID="$(campo_json "['id']")"
IDS_CRIADOS+=("fornecedor #$FORNECEDOR_ID (grupo $ORG_ADMIN)")

CORPO_INVALIDO='{"nome":"Inexistente","cnpj":"11111111111111"}'
codigo="$(requisicao_org POST /api/fornecedores "$CORPO_INVALIDO" "$TOKEN" "$ORG_ADMIN")"
assert_status "CNPJ inválido é rejeitado" 400 "$codigo" || true

codigo="$(requisicao_org GET "/api/fornecedores?nome=$SUFIXO" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "busca de fornecedor por nome retorna o fornecedor criado" 200 "$codigo" || true

# ── 4. Licitação e vencedor ─────────────────────────────────────────────────
verbo "Licitação"
HOJE="$(date +%F)"
CORPO="{\"numeroEdital\":\"SMOKE-$SUFIXO\",\"modalidade\":\"PREGAO\",\"objeto\":\"Objeto do smoke test\",\"dataAbertura\":\"$HOJE\",\"valorEstimado\":30000}"
codigo="$(requisicao_org POST /api/licitacoes "$CORPO" "$TOKEN" "$ORG_ADMIN")"
assert_status "criação da licitação (status ABERTA)" 201 "$codigo" || true
LICITACAO_ID="$(campo_json "['id']")"

codigo="$(requisicao_org PUT "/api/licitacoes/$LICITACAO_ID/vencedor" "{\"fornecedorId\":$FORNECEDOR_ID}" "$TOKEN" "$ORG_ADMIN")"
assert_status "definição do vencedor encerra a licitação" 200 "$codigo" || true
STATUS_LICITACAO="$(campo_json "['status']")"
[[ "$STATUS_LICITACAO" == "ENCERRADA" ]] \
    && assert_status "status final da licitação é ENCERRADA" ENCERRADA ENCERRADA \
    || assert_status "status final da licitação é ENCERRADA" ENCERRADA "$STATUS_LICITACAO"
IDS_CRIADOS+=("licitação #$LICITACAO_ID (grupo $ORG_ADMIN)")

codigo="$(requisicao_org GET "/api/licitacoes/$LICITACAO_ID" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "detalhe da licitação encerrada com vencedor" 200 "$codigo" || true

# ── 5. Contrato ─────────────────────────────────────────────────────────────
verbo "Contrato"
INICIO="$(date -d "$INICIO_MES -1 month" +%F)"
CORPO="{\"numero\":\"SMOKE-$SUFIXO\",\"objeto\":\"Contrato do smoke test\",\"valorTotal\":30000,\"duracaoMeses\":3,\"dataInicio\":\"$INICIO\",\"dotacaoId\":$DOTACAO_ID,\"fornecedorId\":$FORNECEDOR_ID,\"licitacaoId\":$LICITACAO_ID}"
codigo="$(requisicao_org POST /api/contratos "$CORPO" "$TOKEN" "$ORG_ADMIN")"
assert_status "criação do contrato vinculado à tríade dotação+fornecedor+licitação" 201 "$codigo" || true
CONTRATO_ID="$(campo_json "['id']")"
VALOR_MENSAL="$(campo_json "['valorMensal']")"
IDS_CRIADOS+=("contrato #$CONTRATO_ID (grupo $ORG_ADMIN)")
echo "  · valor mensal calculado pelo sistema: R$ $VALOR_MENSAL"

codigo="$(requisicao_org GET "/api/contratos/$CONTRATO_ID" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "detalhe do contrato com valor mensal calculado" 200 "$codigo" || true

# ── 6. Empenhos por competência ─────────────────────────────────────────────
verbo "Empenhos"
CORPO="{\"contratoId\":$CONTRATO_ID,\"mesReferencia\":$MES_ANTERIOR,\"anoReferencia\":$ANO_ANTERIOR}"
codigo="$(requisicao_org POST /api/empenhos "$CORPO" "$TOKEN" "$ORG_ADMIN")"
assert_status "empenho da competência pendente $(printf '%02d' "$MES_ANTERIOR")/$ANO_ANTERIOR" 201 "$codigo" || true
EMPENHO_1="$(campo_json "['id']")"
VALOR_EMPENHO="$(campo_json "['valor']")"
echo "  · valor empenhado: R$ $VALOR_EMPENHO"

CORPO_SEGUNDO="{\"contratoId\":$CONTRATO_ID,\"mesReferencia\":$MES,\"anoReferencia\":$ANO}"
codigo="$(requisicao_org POST /api/empenhos "$CORPO_SEGUNDO" "$TOKEN" "$ORG_ADMIN")"
assert_status "empenho da competência corrente $(printf '%02d' "$MES")/$ANO" 201 "$codigo" || true
EMPENHO_2="$(campo_json "['id']")"

codigo="$(requisicao_org GET "/api/empenhos?contratoId=$CONTRATO_ID" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "listagem de empenhos do contrato" 200 "$codigo" || true

codigo="$(requisicao_org GET "/api/empenhos?contratoId=$CONTRATO_ID&mes=$MES_ANTERIOR&ano=$ANO_ANTERIOR" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "filtro de empenhos da competência pendente $(printf '%02d' "$MES_ANTERIOR")/$ANO_ANTERIOR" 200 "$codigo" || true
NUM_EMPENHO_1="$(campo_json "['content'][0]['numero']")"

codigo="$(requisicao_org GET "/api/empenhos?contratoId=$CONTRATO_ID&mes=$MES&ano=$ANO" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "filtro de empenhos da competência corrente $(printf '%02d' "$MES")/$ANO" 200 "$codigo" || true
NUM_EMPENHO_2="$(campo_json "['content'][0]['numero']")"

if python3 -c "
import sys
n1, n2 = int('$NUM_EMPENHO_1'), int('$NUM_EMPENHO_2')
if '${ANO_ANTERIOR}' == '$ANO':
    sys.exit(0 if n2 == n1 + 1 else 1)
else:
    sys.exit(0 if n1 > 0 and n2 > 0 else 1)" 2>/dev/null; then
    PASSOS=$((PASSOS + 1))
    echo "  ✓ empenhos numerados em sequência: #$NUM_EMPENHO_1 → #$NUM_EMPENHO_2 (V11)"
else
    FALHAS=$((FALHAS + 1))
    echo "  ✗ numeração sequencial inesperada: #$NUM_EMPENHO_1 → #$NUM_EMPENHO_2"
fi

CORPO_FUTURO="{\"contratoId\":$CONTRATO_ID,\"mesReferencia\":$MES_SEGUINTE,\"anoReferencia\":$ANO_SEGUINTE}"
codigo="$(requisicao_org POST /api/empenhos "$CORPO_FUTURO" "$TOKEN" "$ORG_ADMIN")"
assert_status "competência futura apenas na competência corrente é rejeitada" 409 "$codigo" || true

codigo="$(requisicao_org POST /api/empenhos "$CORPO" "$TOKEN" "$ORG_ADMIN")"
assert_status "competência duplicada é rejeitada" 409 "$codigo" || true

# ── 7. Anulação e estorno ───────────────────────────────────────────────────
verbo "Anulação"
codigo="$(requisicao_org DELETE "/api/empenhos/$EMPENHO_1" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "anulação do primeiro empenho" 200 "$codigo" || true
STATUS_ANULADO="$(campo_json "['status']")"
[[ "$STATUS_ANULADO" == "ANULADO" ]] \
    && assert_status "status pós-anulação é ANULADO" ANULADO ANULADO \
    || assert_status "status pós-anulação é ANULADO" ANULADO "$STATUS_ANULADO"

codigo="$(requisicao_org DELETE "/api/empenhos/$EMPENHO_1" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "anulação dupla é rejeitada" 409 "$codigo" || true

codigo="$(requisicao_org GET "/api/dotacoes/$DOTACAO_ID/saldo" "" "$TOKEN" "$ORG_ADMIN")"
assert_status "consulta de saldo da dotação" 200 "$codigo" || true
SALDO_FINAL="$(cat "$ARQ_RESPOSTA")"
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

# ── 8. Multitenancy: cadastro público e grupos ───────────────────────────────
verbo "Cadastro público e grupos"
EMAIL_U1="smoke.u1.$SUFIXO@teste.com"
codigo="$(requisicao POST /api/auth/register "{\"nome\":\"Usuario Um\",\"email\":\"$EMAIL_U1\",\"senha\":\"senhaSegura123\"}")"
assert_status "cadastro público sem token (register liberado)" 201 "$codigo" || true

codigo="$(requisicao POST /api/auth/login "{\"email\":\"$EMAIL_U1\",\"senha\":\"senhaSegura123\"}")"
assert_status "login do novo usuário" 200 "$codigo" || true
TOKEN_U1="$(campo_json "['token']")"

codigo="$(requisicao GET /api/auth/me "" "$TOKEN_U1")"
assert_status "novo usuário consulta /auth/me" 200 "$codigo" || true
QTD_ORG="$(tamanho_array)"
codigo_tmp="$(python3 -c "
import sys, json
d = json.load(open('$ARQ_RESPOSTA'))
print(len(d['organizacoes']))" 2>/dev/null)"
[[ "$codigo_tmp" == "1" ]] \
    && assert_status "novo usuário já nasce com seu espaço pessoal" 1 1 \
    || assert_status "novo usuário já nasce com seu espaço pessoal" 1 "$codigo_tmp"
PAPEL_ESPACO="$(python3 -c "
import json
d = json.load(open('$ARQ_RESPOSTA'))
print(d['organizacoes'][0]['papel'])" 2>/dev/null)"
[[ "$PAPEL_ESPACO" == "ADMIN" ]] \
    && assert_status "papel no espaço pessoal é ADMIN" ADMIN ADMIN \
    || assert_status "papel no espaço pessoal é ADMIN" ADMIN "$PAPEL_ESPACO"

NOME_GRUPO="Grupo Smoke $SUFIXO"
codigo="$(requisicao POST /api/organizacoes "{\"nome\":\"$NOME_GRUPO\"}" "$TOKEN_U1")"
assert_status "usuário cria um grupo e vira ADMIN" 201 "$codigo" || true
NOVA_ORG="$(campo_json "['id']")"
PAPEL_CRIADOR="$(campo_json "['papel']")"
[[ "$PAPEL_CRIADOR" == "ADMIN" ]] \
    && assert_status "papel do criador é ADMIN" ADMIN ADMIN \
    || assert_status "papel do criador é ADMIN" ADMIN "$PAPEL_CRIADOR"
IDS_CRIADOS+=("grupo #$NOVA_ORG ($NOME_GRUPO)")

codigo="$(requisicao GET /api/organizacoes "" "$TOKEN_U1")"
assert_status "listagem dos grupos do usuário" 200 "$codigo" || true

# ── 9. Multitenancy: membros e papéis ────────────────────────────────────────
verbo "Membros e papéis"
EMAIL_U2="smoke.u2.$SUFIXO@teste.com"
requisicao POST /api/auth/register "{\"nome\":\"Usuario Dois\",\"email\":\"$EMAIL_U2\",\"senha\":\"senhaSegura123\"}" > /dev/null
requisicao POST /api/auth/login "{\"email\":\"$EMAIL_U2\",\"senha\":\"senhaSegura123\"}" > /dev/null
TOKEN_U2="$(campo_json "['token']")"

codigo="$(requisicao_org POST "/api/organizacoes/$NOVA_ORG/membros" "{\"email\":\"$EMAIL_U2\",\"papel\":\"VISITANTE\"}" "$TOKEN_U1" "$NOVA_ORG")"
assert_status "ADMIN adiciona membro por e-mail (VISITANTE)" 200 "$codigo" || true

codigo="$(requisicao_org GET "/api/organizacoes/$NOVA_ORG/membros" "" "$TOKEN_U1" "$NOVA_ORG")"
assert_status "listagem de membros do grupo" 200 "$codigo" || true

codigo="$(requisicao_org GET "/api/dotacoes" "" "$TOKEN_U2" "$NOVA_ORG")"
assert_status "VISITANTE lê dotações do grupo" 200 "$codigo" || true

codigo="$(requisicao_org POST /api/fornecedores '{"nome":"Proibido","cnpj":"11444777000161"}' "$TOKEN_U2" "$NOVA_ORG")"
assert_status "VISITANTE não cria fornecedor" 403 "$codigo" || true

requisicao GET /api/auth/me "" "$TOKEN_U1" > /dev/null
USUARIO_U1="$(campo_json "['id']")"
codigo="$(requisicao_org PUT "/api/organizacoes/$NOVA_ORG/membros/$USUARIO_U1" '{"papel":"VISITANTE"}' "$TOKEN_U1" "$NOVA_ORG")"
assert_status "criador não pode ser rebaixado" 409 "$codigo" || true

# ── 10. Multitenancy: convites nominais e código de acesso ────────────────────
verbo "Convites nominais e código de acesso"

# Código de acesso da organização (8 caracteres; quem o usa entra como VISITANTE).
codigo="$(requisicao_org GET "/api/organizacoes/$NOVA_ORG/codigo-acesso" "" "$TOKEN_U1" "$NOVA_ORG")"
assert_status "consulta de código de acesso antes da criação" 200 "$codigo" || true
COD_ANTES="$(campo_json "['codigo']")"
[[ "$COD_ANTES" == "None" ]] \
    && assert_status "código de acesso ainda não existe" None None \
    || assert_status "código de acesso ainda não existe" None "$COD_ANTES"

codigo="$(requisicao_org POST "/api/organizacoes/$NOVA_ORG/codigo-acesso" "" "$TOKEN_U1" "$NOVA_ORG")"
assert_status "administrador gera o código de acesso" 200 "$codigo" || true
COD_ACESSO="$(campo_json "['codigo']")"
if [[ ${#COD_ACESSO} -eq 8 ]]; then
    PASSOS=$((PASSOS + 1))
    echo "  ✓ código de acesso gerado com 8 caracteres ($COD_ACESSO)"
else
    FALHAS=$((FALHAS + 1))
    echo "  ✗ código de acesso inesperado: '$COD_ACESSO'"
fi

EMAIL_U3="smoke.u3.$SUFIXO@teste.com"
requisicao POST /api/auth/register "{\"nome\":\"Usuario Tres\",\"email\":\"$EMAIL_U3\",\"senha\":\"senhaSegura123\"}" > /dev/null
requisicao POST /api/auth/login "{\"email\":\"$EMAIL_U3\",\"senha\":\"senhaSegura123\"}" > /dev/null
TOKEN_U3="$(campo_json "['token']")"

codigo="$(requisicao POST "/api/convites/aceitar" "{\"codigo\":\"$COD_ACESSO\"}" "$TOKEN_U3")"
assert_status "usuário entra no grupo pelo código de acesso" 200 "$codigo" || true
PAPEL_CODIGO="$(campo_json "['papel']")"
[[ "$PAPEL_CODIGO" == "VISITANTE" ]] \
    && assert_status "código de acesso concede papel VISITANTE" VISITANTE VISITANTE \
    || assert_status "código de acesso concede papel VISITANTE" VISITANTE "$PAPEL_CODIGO"

codigo="$(requisicao_org POST /api/fornecedores '{"nome":"Proibido","cnpj":"11444777000161"}' "$TOKEN_U3" "$NOVA_ORG")"
assert_status "VISITANTE não cria fornecedor no grupo" 403 "$codigo" || true

codigo="$(requisicao_org DELETE "/api/organizacoes/$NOVA_ORG/codigo-acesso" "" "$TOKEN_U1" "$NOVA_ORG")"
assert_status "administrador revoga o código de acesso" 204 "$codigo" || true

codigo="$(requisicao POST "/api/convites/aceitar" "{\"codigo\":\"$COD_ACESSO\"}" "$TOKEN_U3")"
assert_status "código revogado deixa de aceitar novos membros" 404 "$codigo" || true

# Convite nominal por e-mail: exige conta existente do convidado.
EMAIL_U4="smoke.u4.$SUFIXO@teste.com"
requisicao POST /api/auth/register "{\"nome\":\"Usuario Quatro\",\"email\":\"$EMAIL_U4\",\"senha\":\"senhaSegura123\"}" > /dev/null
requisicao POST /api/auth/login "{\"email\":\"$EMAIL_U4\",\"senha\":\"senhaSegura123\"}" > /dev/null
TOKEN_U4="$(campo_json "['token']")"

codigo="$(requisicao_org POST "/api/organizacoes/$NOVA_ORG/convites" "{\"email\":\"$EMAIL_U4\",\"papel\":\"OPERADOR\"}" "$TOKEN_U1" "$NOVA_ORG")"
assert_status "convite nominal por e-mail criado (OPERADOR)" 200 "$codigo" || true

codigo="$(requisicao_org POST "/api/organizacoes/$NOVA_ORG/convites" "{\"codigo\":\"ANTIGO-$SUFIXO\",\"papel\":\"OPERADOR\"}" "$TOKEN_U1" "$NOVA_ORG")"
assert_status "criar convite por código é rejeitado (modelo novo)" 400 "$codigo" || true

codigo="$(requisicao GET /api/convites/pendentes "" "$TOKEN_U4")"
assert_status "usuário lista convites pendentes do próprio e-mail" 200 "$codigo" || true
QTD_PENDENTES="$(tamanho_array)"
[[ "$QTD_PENDENTES" == "1" ]] \
    && assert_status "há 1 convite pendente para o usuário quatro" 1 1 \
    || assert_status "há 1 convite pendente para o usuário quatro" 1 "$QTD_PENDENTES"
CONVITE_U4="$(python3 -c "
import json
d = json.load(open('$ARQ_RESPOSTA'))
print(d[0]['id'] if d else '')" 2>/dev/null)"
if [[ -n "$CONVITE_U4" ]]; then
    PASSOS=$((PASSOS + 1))
    echo "  ✓ convite pendente localizado (id $CONVITE_U4)"
else
    FALHAS=$((FALHAS + 1))
    echo "  ✗ convite pendente não localizado"
fi

codigo="$(requisicao POST "/api/convites/$CONVITE_U4/aceitar" "" "$TOKEN_U4")"
assert_status "usuário aceita convite nominal" 200 "$codigo" || true
PAPEL_NOMINAL="$(campo_json "['papel']")"
[[ "$PAPEL_NOMINAL" == "OPERADOR" ]] \
    && assert_status "convite nominal concedeu o papel do convite" OPERADOR OPERADOR \
    || assert_status "convite nominal concedeu o papel do convite" OPERADOR "$PAPEL_NOMINAL"

codigo="$(requisicao_org POST /api/fornecedores "{\"nome\":\"Fornecedor Operador $SUFIXO\",\"cnpj\":\"$(gerar_cnpj_valido)\",\"email\":\"op$SUFIXO@teste.com\"}" "$TOKEN_U4" "$NOVA_ORG")"
assert_status "OPERADOR (via convite nominal) cria fornecedor no grupo" 201 "$codigo" || true

codigo="$(requisicao POST "/api/convites/$CONVITE_U4/aceitar" "" "$TOKEN_U4")"
assert_status "convite nominal já aceito é rejeitado" 404 "$codigo" || true

# Recusa de convite nominal.
EMAIL_U5="smoke.u5.$SUFIXO@teste.com"
requisicao POST /api/auth/register "{\"nome\":\"Usuario Cinco\",\"email\":\"$EMAIL_U5\",\"senha\":\"senhaSegura123\"}" > /dev/null
requisicao POST /api/auth/login "{\"email\":\"$EMAIL_U5\",\"senha\":\"senhaSegura123\"}" > /dev/null
TOKEN_U5="$(campo_json "['token']")"

codigo="$(requisicao_org POST "/api/organizacoes/$NOVA_ORG/convites" "{\"email\":\"$EMAIL_U5\",\"papel\":\"VISITANTE\"}" "$TOKEN_U1" "$NOVA_ORG")"
assert_status "convite nominal para usuário cinco criado" 200 "$codigo" || true

codigo="$(requisicao GET /api/convites/pendentes "" "$TOKEN_U5")"
assert_status "usuário cinco lista os convites pendentes" 200 "$codigo" || true
CONVITE_U5="$(python3 -c "
import json
d = json.load(open('$ARQ_RESPOSTA'))
print(d[0]['id'] if d else '')" 2>/dev/null)"
if [[ -n "$CONVITE_U5" ]]; then
    PASSOS=$((PASSOS + 1))
    echo "  ✓ convite pendente localizado para recusa (id $CONVITE_U5)"
else
    FALHAS=$((FALHAS + 1))
    echo "  ✗ convite pendente não localizado para recusa"
fi

codigo="$(requisicao POST "/api/convites/$CONVITE_U5/recusar" "" "$TOKEN_U5")"
assert_status "usuário recusa convite nominal" 204 "$codigo" || true

codigo="$(requisicao POST "/api/convites/$CONVITE_U5/recusar" "" "$TOKEN_U5")"
assert_status "recusa duplicada é rejeitada" 409 "$codigo" || true

codigo="$(requisicao_org GET /api/dotacoes "" "$TOKEN_U5" "$NOVA_ORG")"
assert_status "quem recusou o convite não entra no grupo" 403 "$codigo" || true

codigo="$(requisicao_org GET "/api/organizacoes/$NOVA_ORG/convites" "" "$TOKEN_U1" "$NOVA_ORG")"
assert_status "listagem de convites da organização (pendentes + recusados)" 200 "$codigo" || true

# ── 11. Isolamento por X-Org-Id e painel do super admin ─────────────────────
verbo "Isolamento e painel SUPER_ADMIN"
codigo="$(requisicao_org GET "/api/dotacoes" "" "$TOKEN_U4" "$NOVA_ORG")"
assert_status "membro acessa dados do grupo com X-Org-Id" 200 "$codigo" || true

codigo="$(requisicao_org GET "/api/dotacoes" "" "$TOKEN_U3" "$ORG_ADMIN")"
assert_status "não-membro recebe 403 (X-Org-Id de outro grupo)" 403 "$codigo" || true

codigo="$(requisicao GET "/api/dotacoes" "" "$TOKEN_U4")"
assert_status "membro sem X-Org-Id não acessa dados" 403 "$codigo" || true

codigo="$(requisicao GET /api/admin/usuarios "" "$TOKEN")"
assert_status "painel: super admin lista usuários" 200 "$codigo" || true
codigo="$(requisicao GET /api/admin/organizacoes "" "$TOKEN")"
assert_status "painel: super admin lista organizações com total de membros" 200 "$codigo" || true
codigo="$(requisicao GET /api/admin/usuarios "" "$TOKEN_U1")"
assert_status "painel: usuário comum é barrado" 403 "$codigo" || true

# ── 12. Redefinição de senha (pública e pelo ADMIN do grupo) ─────────────────
verbo "Redefinição de senha"

# Pública: confirma e-mail + senha atual e revoga todas as sessões do usuário.
codigo="$(requisicao POST /api/auth/redefinir-senha \
    "{\"email\":\"$EMAIL_U2\",\"senhaAtual\":\"errada\",\"novaSenha\":\"senhaNova123\"}")"
assert_status "redefinição com senha atual incorreta é rejeitada" 400 "$codigo" || true

codigo="$(requisicao POST /api/auth/redefinir-senha \
    "{\"email\":\"naoexiste.$SUFIXO@teste.com\",\"senhaAtual\":\"senhaSegura123\",\"novaSenha\":\"senhaNova123\"}")"
assert_status "redefinição com e-mail inexistente é rejeitada" 400 "$codigo" || true

codigo="$(requisicao POST /api/auth/redefinir-senha \
    "{\"email\":\"$EMAIL_U2\",\"senhaAtual\":\"senhaSegura123\",\"novaSenha\":\"senhaNova123\"}")"
assert_status "redefinição pública com senha atual correta" 200 "$codigo" || true

codigo="$(requisicao POST /api/auth/login "{\"email\":\"$EMAIL_U2\",\"senha\":\"senhaNova123\"}")"
assert_status "login com a nova senha após a redefinição" 200 "$codigo" || true

codigo="$(requisicao POST /api/auth/login "{\"email\":\"$EMAIL_U2\",\"senha\":\"senhaSegura123\"}")"
assert_status "senha antiga deixa de funcionar" 401 "$codigo" || true

codigo="$(requisicao GET /api/auth/me "" "$TOKEN_U2")"
assert_status "sessão anterior é revogada pela redefinição" 401 "$codigo" || true

# Pelo ADMIN do grupo: sem confirmação de senha; o alvo precisa ser membro.
requisicao GET /api/auth/me "" "$TOKEN_U3" > /dev/null
USUARIO_U3="$(campo_json "['id']")"
requisicao GET /api/auth/me "" "$TOKEN" > /dev/null
USUARIO_SUPER="$(campo_json "['id']")"

codigo="$(requisicao_org PUT "/api/organizacoes/$NOVA_ORG/membros/$USUARIO_U3/senha" \
    '{"novaSenha":"senhaAdmin123"}' "$TOKEN_U4" "$NOVA_ORG")"
assert_status "OPERADOR não redefine senha de membro" 403 "$codigo" || true

codigo="$(requisicao_org PUT "/api/organizacoes/$NOVA_ORG/membros/$USUARIO_U1/senha" \
    '{"novaSenha":"senhaAdmin123"}' "$TOKEN_U1" "$NOVA_ORG")"
assert_status "ADMIN não redefine a própria senha" 409 "$codigo" || true

codigo="$(requisicao_org PUT "/api/organizacoes/$NOVA_ORG/membros/$USUARIO_SUPER/senha" \
    '{"novaSenha":"senhaAdmin123"}' "$TOKEN_U1" "$NOVA_ORG")"
assert_status "alvo que não é membro do grupo não é encontrado" 404 "$codigo" || true

codigo="$(requisicao_org PUT "/api/organizacoes/$NOVA_ORG/membros/$USUARIO_U3/senha" \
    '{"novaSenha":"senhaAdmin123"}' "$TOKEN_U1" "$NOVA_ORG")"
assert_status "ADMIN redefine a senha de um membro" 204 "$codigo" || true

codigo="$(requisicao POST /api/auth/login "{\"email\":\"$EMAIL_U3\",\"senha\":\"senhaAdmin123\"}")"
assert_status "membro entra com a senha definida pelo ADMIN" 200 "$codigo" || true

codigo="$(requisicao GET /api/auth/me "" "$TOKEN_U3")"
assert_status "sessões anteriores do membro são revogadas" 401 "$codigo" || true

# ── Encerramento ────────────────────────────────────────────────────────────
rm -f "$ARQ_RESPOSTA"
echo "════════════════════════════════════════════════════════════"
echo " Resultado: $PASSOS verificações OK, $FALHAS falha(s)"
echo " Registros criados nesta execução (histórico permanente):"
for item in "${IDS_CRIADOS[@]}"; do echo "   · $item"; done
echo "════════════════════════════════════════════════════════════"
exit $(( FALHAS > 0 ? 1 : 0 ))