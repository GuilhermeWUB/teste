-- =====================================================
-- CONFIGURAÇÃO DE USUÁRIO DE BANCO DE DADOS SOMENTE LEITURA
-- =====================================================
--
-- Este script configura um usuário de banco de dados com permissões
-- APENAS de leitura (SELECT) para ser usado com a funcionalidade de
-- Relatórios Dinâmicos com IA.
--
-- IMPORTANTE: Este usuário deve ser usado em PRODUÇÃO para garantir
-- que queries SQL geradas pela IA não possam modificar dados.
--
-- =====================================================

-- 1. Criar usuário (se ainda não existir)
-- Senha: leitor123 (ALTERE EM PRODUÇÃO!)
CREATE USER sub_leitor WITH PASSWORD 'leitor123';

-- 2. Conceder permissão de CONEXÃO ao banco de dados
-- Substitua 'seu_banco_de_dados' pelo nome real do seu banco
GRANT CONNECT ON DATABASE seu_banco_de_dados TO sub_leitor;

-- 3. Conceder permissão de USO do schema public
GRANT USAGE ON SCHEMA public TO sub_leitor;

-- 4. Conceder permissão SELECT em TODAS as tabelas existentes
GRANT SELECT ON ALL TABLES IN SCHEMA public TO sub_leitor;

-- 5. Conceder permissão SELECT em tabelas FUTURAS (auto-grant)
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO sub_leitor;

-- 6. Conceder permissão SELECT em SEQUENCES (para queries que usam sequences)
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO sub_leitor;

-- 7. VERIFICAÇÃO: Listar permissões do usuário
-- Execute esta query para verificar as permissões concedidas:
SELECT
    grantee,
    table_schema,
    table_name,
    privilege_type
FROM
    information_schema.table_privileges
WHERE
    grantee = 'sub_leitor'
ORDER BY
    table_name;

-- =====================================================
-- COMO USAR EM PRODUÇÃO
-- =====================================================
--
-- Para usar este usuário com a funcionalidade de Relatórios com IA,
-- você tem duas opções:
--
-- OPÇÃO 1: Usar um DataSource separado (mais seguro)
-- Configure um segundo DataSource no Spring Boot especificamente
-- para relatórios com IA. Isso requer modificações no código.
--
-- OPÇÃO 2: Usar como usuário principal em produção
-- Altere o application.properties (ou application-prod.properties):
--
-- spring.datasource.username=sub_leitor
-- spring.datasource.password=leitor123
--
-- NOTA: A OPÇÃO 2 limitará TODAS as operações do sistema a SELECT apenas,
-- o que NÃO é desejado. Portanto, recomenda-se a OPÇÃO 1.
--
-- =====================================================
-- SEGURANÇA ADICIONAL
-- =====================================================
--
-- 1. ALTERE A SENHA em produção para algo mais forte:
--    ALTER USER sub_leitor WITH PASSWORD 'senha_forte_aqui';
--
-- 2. Limite conexões do usuário se necessário:
--    ALTER USER sub_leitor CONNECTION LIMIT 10;
--
-- 3. Revogue permissões em tabelas sensíveis se houver:
--    REVOKE SELECT ON table_sensivel FROM sub_leitor;
--
-- 4. Monitore queries executadas pelo usuário:
--    SELECT * FROM pg_stat_activity WHERE usename = 'sub_leitor';
--
-- =====================================================
-- REVOGAR PERMISSÕES (se necessário)
-- =====================================================
--
-- Para remover todas as permissões:
-- REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM sub_leitor;
-- REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM sub_leitor;
-- REVOKE USAGE ON SCHEMA public FROM sub_leitor;
-- REVOKE CONNECT ON DATABASE seu_banco_de_dados FROM sub_leitor;
-- DROP USER sub_leitor;
--
-- =====================================================
