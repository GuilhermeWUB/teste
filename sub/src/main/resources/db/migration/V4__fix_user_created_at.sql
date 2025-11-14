-- ============================================
-- Script de Correcao: created_at NULL
-- Tabela: app_users
-- ============================================
-- Este script corrige valores NULL na coluna created_at
-- e adiciona protecoes para prevenir o problema no futuro
-- ============================================

-- PASSO 1: Corrigir valores NULL existentes
-- Atualiza todos os registros com created_at NULL para usar a data/hora atual
UPDATE app_users
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

-- PASSO 2: Adicionar valor DEFAULT na coluna (se ainda nao tiver)
-- Isso garante que novos registros sempre tenham um valor valido
ALTER TABLE app_users
ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

-- PASSO 3: Garantir que a coluna seja NOT NULL (se ainda nao for)
-- Previne que valores NULL sejam inseridos no futuro
ALTER TABLE app_users
ALTER COLUMN created_at SET NOT NULL;

-- ============================================
-- Verificacao (executar apos o script)
-- ============================================
-- Execute esta query para verificar se ainda ha registros com created_at NULL:
-- SELECT COUNT(*) FROM app_users WHERE created_at IS NULL;
-- Resultado esperado: 0
-- ============================================
