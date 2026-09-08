-- Em bancos criados antes do Flyway (baseline a partir do V1), as uniques
-- globais originais foram criadas pelo Hibernate `ddl-auto` com nomes
-- gerados (ex.: uk4waq31m882y0udrmwdb9lvtfo). A V4 derrubava pelos nomes do
-- V1__init (uk_dotacoes_codigo, uk_fornecedores_cnpj, ...), que NÃO existem
-- nesses bancos — e a global remanescente quebrava o multitenancy
-- (CNPJ/código/edital/número continuavam únicos entre TODOS os grupos).
--
-- Esta migração derruba, dinamicamente, qualquer UNIQUE de coluna única das
-- 4 tabelas de negócio que não seja uma única composta por organização.
-- Em bancos novos (V1..V4 aplicados sequencialmente) não encontra nada.
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT c.conrelid::regclass AS tabela, c.conname
        FROM pg_constraint c
        JOIN pg_class     t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE c.contype = 'u'
          AND n.nspname = 'public'
          AND c.conrelid IN
              ('dotacoes_orcamentarias'::regclass,
               'fornecedores'::regclass,
               'licitacoes'::regclass,
               'contratos'::regclass)
          AND array_length(c.conkey, 1) = 1
          AND c.conname NOT LIKE 'uk_%_org_%'
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT IF EXISTS %I',
                       r.tabela, r.conname);
        RAISE NOTICE 'unique global legada removida: %.%', r.tabela, r.conname;
    END LOOP;
END $$;