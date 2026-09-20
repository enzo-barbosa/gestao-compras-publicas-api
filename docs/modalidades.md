# Modalidades de licitação

> O enum `ModalidadeLicitacao` do backend define as **9 modalidades** aceitas no campo `modalidade` de uma licitação (formato `LETRAS_MAIUSCULAS`, validado com mensagem específica de enum no `GlobalExceptionHandler`).

## Origem legal

- **8 modalidades** vêm da **Lei 8.666/1993** (art. 22): concorrência, tomada de preços, convite, concurso e leilão (as cinco clássicas) + as três derivações de contratação direta previstas no art. 24/25 (dispensa e inexigibilidade).
- **1 modalidade nova** vem da **Lei 14.133/2021** (art. 28, IV): **diálogo competitivo** — a inovação que a lei nova trouxe para contratações complexas com soluções ainda não definidas.
- O **pregão** (Lei 10.520/2002, regulamentada pela Lei 14.133/2021) permanece como a modalidade mais usada na prática, especialmente na forma **eletrônica**.

## As 9 modalidades

| Modalidade (enum) | Lei de origem | Critério usado para vencer | Quando usar |
|---|---|---|---|
| `PREGAO` | Lei 10.520/2002 + 14.133/2021 | Menor preço (proposta + lances) | Objetos comuns e padronizados, com especificação objetiva no edital |
| `CONCORRENCIA` | 8.666/1993, art. 22, I | Menor preço, melhor técnica, técnica e preço | Compras/serviços de maior vulto ou critérios técnicos relevantes |
| `TOMADA_DE_PRECOS` | 8.666/1993, art. 22, II | Menor preço ou técnica e preço | Licitações com valores médios, habilitação prévia documental |
| `CONVITE` | 8.666/1993, art. 22, III | Menor preço | Valores menores; convidados escolhidos + ampla publicidade para demais interessados |
| `CONCURSO` | 8.666/1993, art. 22, IV | Melhor técnica (trabalho técnico, científico ou artístico) | Escolha de trabalho técnico/científico, prêmios a projetos |
| `LEILAO` | 8.666/1993, art. 22, V | Maior lance ou oferta | Venda de bens móveis/móveis, alienação de bens |
| `DIALOGO_COMPETITIVO` | 14.133/2021, art. 28, IV | Solução + condições mais vantajosas (diálogo em fases) | Contratações de alta complexidade, soluções inovadoras não definidas de antemão |
| `DISPENSA` | 8.666/1993, art. 24 | Contratação direta (sem licitação) | Casos legais taxativos: baixo valor, emergência, etc. |
| `INEXIGIBILIDADE` | 8.666/1993, art. 25 | Contratação direta (sem licitação) | Fornecedor exclusivo ou notoriamente especializado (inviabilidade de competição) |

## No sistema

- O campo `modalidade` é obrigatório na criação da licitação (`POST /api/licitacoes`), checado contra o enum com mensagem dedicada (ex.: valor inválido → `400`).
- O `LicitacaoService` não aplica regras de **vulto/valor** por modalidade — a escolha da modalidade é decisão do órgão (o sistema armazena e audita, não arbitra se a modalidade é a "correta" para o valor estimado). Isso fica registrado como **evolução futura** (validação por faixa de valor conforme os limites legais da 8.666/14.133).
- As modalidades alimentam o **diagrama `fluxo-licitacao`** e os **relatórios de listagem** (`GET /api/licitacoes?modalidade=...`).
