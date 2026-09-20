--
-- V11 — Numeração sequencial de empenhos por organização + ano.
--
-- Empenho ganha a coluna `numero` (sequencial por (organizacao, ano),
-- SEM reúso: um número anulado não é reaproveitado). A sequência é mantida
-- na tabela empenhos_sequencias (linha por (organizacao, ano)) e lida com
-- lock pessimista no momento da emissão.
--
-- 1. Backfill dos empenhos existentes em ordem estável (data_emissao, id).
-- 2. NOT NULL.
-- 3. Tabela de sequências.
-- 4. Unique (organizacao_id, ano_referencia, numero) que garante a regra
--    "um número por ano dentro do mesmo grupo".
--

ALTER TABLE public.empenhos ADD COLUMN numero integer;

WITH ordenados AS (
    SELECT id, organizacao_id, ano_referencia,
           row_number() OVER (
               PARTITION BY organizacao_id, ano_referencia
               ORDER BY data_emissao, id
           ) AS sequencia
    FROM public.empenhos
)
UPDATE public.empenhos e
SET numero = o.sequencia
FROM ordenados o
WHERE e.id = o.id;

ALTER TABLE public.empenhos
    ALTER COLUMN numero SET NOT NULL;

CREATE TABLE public.empenhos_sequencias (
    organizacao_id bigint NOT NULL REFERENCES public.organizacoes(id),
    ano_referencia integer NOT NULL,
    ultimo integer NOT NULL,
    CONSTRAINT empenhos_sequencias_pkey PRIMARY KEY (organizacao_id, ano_referencia)
);

ALTER TABLE public.empenhos
    ADD CONSTRAINT uk_empenhos_org_ano_numero
    UNIQUE (organizacao_id, ano_referencia, numero);