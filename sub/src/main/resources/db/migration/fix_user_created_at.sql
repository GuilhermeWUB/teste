-- ========================================
-- CORREÇÃO COMPLETA: Erro de TimeStamp na aba de usuários
-- ========================================
-- Este script resolve o problema de valores NULL na coluna created_at
-- que causam erro ao acessar a página de usuários do sistema

-- Passo 1: Corrige TODOS os registros existentes que têm created_at NULL
UPDATE app_users
SET created_at = COALESCE(created_at, NOW())
WHERE created_at IS NULL;

-- Passo 2: Adiciona um valor DEFAULT para novos registros (se não existir)
ALTER TABLE app_users
ALTER COLUMN created_at SET DEFAULT NOW();

-- Passo 3: Garante que a coluna é NOT NULL
ALTER TABLE app_users
ALTER COLUMN created_at SET NOT NULL;

-- ========================================
-- VERIFICAÇÃO
-- ========================================
-- Execute este comando para verificar se funcionou:
-- SELECT username, created_at FROM app_users WHERE created_at IS NULL;
-- Deve retornar 0 registros
