--
-- V9 — Remove a recuperação de senha por código enviado por e-mail.
--
-- A redefinição de senha passou a validar a senha atual informada pelo próprio
-- usuário; não há mais envio de e-mail nem armazenamento de tokens/códigos.
--

DROP TABLE IF EXISTS public.recuperacao_senha;
