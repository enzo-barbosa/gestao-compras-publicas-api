--
-- V6 — Versão de token para revogação de sessões (logout em todos os dispositivos).
--
-- Cada usuário carrega um contador `versao_token`. O JWT assinado carrega esse
-- valor na claim `vt`; o filtro rejeita tokens com vt diferente do banco.
-- Trocar a senha ou "sair em todos os dispositivos" incrementa o contador e
-- invalida todos os tokens emitidos antes disso.
--

ALTER TABLE public.usuarios
    ADD COLUMN versao_token integer NOT NULL DEFAULT 0;