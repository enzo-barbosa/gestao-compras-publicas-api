--
-- V8 — Unicidade do nome da organização garantida no banco.
--
-- A checagem de nome já existia na aplicação (equalsIgnoreCase/trim), mas era
-- um full scan em organizacaoRepository.findAll() e não impedia duas criações
-- simultâneas de gravarem o mesmo nome. Este índice único funcional fecha a
-- corrida no banco e casa com OrganizacaoRepository.existsByNomeIgnoreCase.
--

CREATE UNIQUE INDEX uk_organizacoes_nome_ci
    ON public.organizacoes (lower(nome));
