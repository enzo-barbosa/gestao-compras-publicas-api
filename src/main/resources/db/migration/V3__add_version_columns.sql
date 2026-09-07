--
-- V3 — Colunas de versão para locking otimista (integridade financeira).
--
-- @Version em DotacaoOrcamentaria e Contrato: qualquer escrita concorrente sobre
-- saldos detecta conflito e falha com 409 em vez de corromper os valores.
--

ALTER TABLE public.dotacoes_orcamentarias ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE public.contratos ADD COLUMN version bigint NOT NULL DEFAULT 0;