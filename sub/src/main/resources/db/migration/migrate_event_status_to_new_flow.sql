-- ===============================================
-- Migração: Atualizar Status de Eventos
-- ===============================================
-- Data: 2025-11-10
-- Descrição: Remove os status antigos (A_FAZER, EM_ANDAMENTO, AGUARDANDO, CONCLUIDO)
--            e prepara o banco para os novos 19 status do fluxo de sinistro
--
-- IMPORTANTE:
-- 1. Faça backup da tabela 'event' antes de executar este script
-- 2. Este script APAGA todos os eventos existentes
-- 3. Execute em ambiente de desenvolvimento primeiro
-- ===============================================

-- Passo 1: Criar backup da tabela (recomendado)
-- Execute manualmente antes desta migração:
-- CREATE TABLE event_backup AS SELECT * FROM event;

-- Passo 2: Limpar todos os eventos existentes
-- Conforme solicitado pelo usuário, todos os eventos antigos serão removidos
DELETE FROM event;

-- Passo 3: Resetar a sequência de IDs (PostgreSQL)
ALTER SEQUENCE IF EXISTS event_id_seq RESTART WITH 1;

-- Passo 4: Verificar se a coluna status existe e está configurada corretamente
-- A coluna 'status' deve ser do tipo VARCHAR para suportar os novos valores enum

-- ===============================================
-- NOVOS STATUS DISPONÍVEIS:
-- ===============================================
-- Fase 1 - Comunicação:
--   COMUNICADO (1.0)
--   ABERTO (1.1)
--
-- Fase 2 - Análise:
--   VISTORIA (2.0)
--   ANALISE (2.1)
--   SINDICANCIA (2.2)
--   DESISTENCIA (2.8)
--
-- Fase 3 - Negociação:
--   ORCAMENTO (3.0)
--   COTA_PARTICIPACAO (3.1)
--   ACORDO_ANDAMENTO (3.2)
--
-- Fase 4 - Execução:
--   COMPRA (4.0)
--   AGENDADO (4.1)
--   REPAROS_LIBERADOS (4.2)
--   COMPLEMENTOS (4.3)
--   ENTREGUES (4.7)
--   PESQUISA_SATISFACAO (4.8)
--
-- Fase 5 - Garantia:
--   ABERTURA_GARANTIA (5.0)
--   VISTORIA_GARANTIA (5.1)
--   GARANTIA_AUTORIZADA (5.2)
--   GARANTIA_ENTREGUE (5.7)
-- ===============================================

-- Passo 5: Adicionar constraint para validar os novos status (opcional)
-- Descomente se quiser adicionar validação no banco de dados
/*
ALTER TABLE event DROP CONSTRAINT IF EXISTS event_status_check;
ALTER TABLE event ADD CONSTRAINT event_status_check
CHECK (status IN (
    'COMUNICADO', 'ABERTO',
    'VISTORIA', 'ANALISE', 'SINDICANCIA', 'DESISTENCIA',
    'ORCAMENTO', 'COTA_PARTICIPACAO', 'ACORDO_ANDAMENTO',
    'COMPRA', 'AGENDADO', 'REPAROS_LIBERADOS', 'COMPLEMENTOS', 'ENTREGUES', 'PESQUISA_SATISFACAO',
    'ABERTURA_GARANTIA', 'VISTORIA_GARANTIA', 'GARANTIA_AUTORIZADA', 'GARANTIA_ENTREGUE'
));
*/

-- Migração concluída!
-- A tabela 'event' está pronta para receber eventos com os novos status.
