--
-- V2 — Índices para os filtros quentes + correção do unique de empenho.
--
-- Contexto: o banco carregava um UNIQUE incondicional em empenhos
-- (contrato_id, ano_referencia, mes_referencia) criado por versão antiga da
-- entidade. Ele impede recriar um empenho após anulação. Substituímos por um
-- índice UNIQUE PARCIAL que só protege status ativos — o ANULADO deixa de
-- bloquear a recriação (regra de negócio do módulo de empenhos).
--

ALTER TABLE public.empenhos DROP CONSTRAINT IF EXISTS uk_empenho_contrato_competencia;

-- Filtros de listagem mais usados (FKs e competência)
CREATE INDEX IF NOT EXISTS idx_empenhos_contrato ON public.empenhos(contrato_id);
CREATE INDEX IF NOT EXISTS idx_empenhos_competencia ON public.empenhos(ano_referencia, mes_referencia);
CREATE INDEX IF NOT EXISTS idx_contratos_dotacao ON public.contratos(dotacao_id);
CREATE INDEX IF NOT EXISTS idx_contratos_fornecedor ON public.contratos(fornecedor_id);
CREATE INDEX IF NOT EXISTS idx_contratos_status ON public.contratos(status);
CREATE INDEX IF NOT EXISTS idx_licitacoes_vencedor ON public.licitacoes(fornecedor_vencedor_id);
CREATE INDEX IF NOT EXISTS idx_creditos_origem ON public.creditos_suplementares(dotacao_origem_id);
CREATE INDEX IF NOT EXISTS idx_creditos_destino ON public.creditos_suplementares(dotacao_destino_id);
CREATE INDEX IF NOT EXISTS idx_movimentacoes_dotacao ON public.movimentacoes_dotacao(dotacao_id);

-- Garantia de banco contra empenho duplicado ativo (a aplicação também valida, com 409 amigável)
CREATE UNIQUE INDEX IF NOT EXISTS uk_empenho_ativo_competencia
    ON public.empenhos(contrato_id, ano_referencia, mes_referencia)
    WHERE status IN ('EMPENHADO', 'LIQUIDADO', 'PAGO');