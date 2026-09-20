--
-- V10 — Convites universais e convites nominais com recusa.
--
-- organizacoes.codigo_acesso: código único (por organização) que permite
-- a qualquer usuário autenticado entrar no grupo como VISITANTE; criado e
-- revogado pelo ADMIN. Único mecanismo de "código" do sistema — os convites
-- por código individual foram substituídos por ele.
-- convites_organizacao.recusado_em: convite nominal (por e-mail) pode ser
-- aceito OU recusado pelo convidado.
--

ALTER TABLE public.organizacoes
    ADD COLUMN codigo_acesso character varying(24);

CREATE UNIQUE INDEX uk_organizacoes_codigo_acesso
    ON public.organizacoes(codigo_acesso) WHERE codigo_acesso IS NOT NULL;

ALTER TABLE public.convites_organizacao
    ADD COLUMN recusado_em timestamp(6) without time zone;