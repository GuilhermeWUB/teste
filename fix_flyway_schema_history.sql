-- ===============================================
-- SCRIPT DE CORREÇÃO: Flyway Schema History
-- ===============================================
-- Este script corrige o problema de migrações que foram
-- executadas manualmente mas não foram registradas no Flyway.
--
-- PROBLEMA:
-- A tabela 'vistoria' já existe no banco, mas o Flyway não
-- sabe disso porque a migração V14 não está registrada na
-- tabela 'flyway_schema_history'.
--
-- SOLUÇÃO:
-- Vamos registrar manualmente todas as migrações que já
-- foram aplicadas no banco de dados.
--
-- ===============================================

-- PASSO 1: Verificar estado atual do Flyway
-- Execute esta query para ver quais migrações o Flyway conhece:
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- ===============================================
-- PASSO 2: Inserir registros de migrações faltantes
-- ===============================================

-- IMPORTANTE: Execute apenas os INSERTs das migrações que
-- NÃO aparecem na consulta do PASSO 1, mas cujas tabelas
-- já existem no seu banco.

-- V14: Criar tabela vistoria
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '14',
    'create vistoria table',
    'SQL',
    'V14__create_vistoria_table.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '14'
);

-- V15: Criar tabela vistoria_foto
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '15',
    'create vistoria foto table',
    'SQL',
    'V15__create_vistoria_foto_table.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '15'
);

-- V16: Adicionar completion_observation às demandas
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '16',
    'add completion observation to demands',
    'SQL',
    'V16__add_completion_observation_to_demands.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '16'
);

-- V17: Criar tabela bill_to_pay
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '17',
    'create bill to pay table',
    'SQL',
    'V17__create_bill_to_pay_table.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '17'
);

-- V18: Criar tabela agreements
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '18',
    'create agreements table',
    'SQL',
    'V18__create_agreements_table.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '18'
);

-- V19: Adicionar pdf_path às tabelas financeiras
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '19',
    'add pdf columns to billing tables',
    'SQL',
    'V19__add_pdf_columns_to_billing_tables.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '19'
);

-- V20: Criar tabela fiscal_document
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '20',
    'create fiscal document table',
    'SQL',
    'V20__create_fiscal_document_table.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '20'
);

-- V21: Adicionar placa ao fiscal_document
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '21',
    'add placa to fiscal document',
    'SQL',
    'V21__add_placa_to_fiscal_document.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '21'
);

-- V22: Adicionar CNPJ ao partner
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '22',
    'add cnpj to partner',
    'SQL',
    'V22__add_cnpj_to_partner.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '22'
);

-- V23: Criar tabela nfe_config
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '23',
    'create nfe config table',
    'SQL',
    'V23__create_nfe_config_table.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '23'
);

-- V24: Criar tabela incoming_invoice
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '24',
    'create incoming invoice table',
    'SQL',
    'V24__create_incoming_invoice_table.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '24'
);

-- V25: Criar tabela sales
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '25',
    'create sales table',
    'SQL',
    'V25__create_sales_table.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '25'
);

-- V26: Atualizar status de sales
INSERT INTO flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    installed_on,
    execution_time,
    success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '26',
    'update sales status to affiliation funnel',
    'SQL',
    'V26__update_sales_status_to_affiliation_funnel.sql',
    -1,
    CURRENT_USER,
    CURRENT_TIMESTAMP,
    0,
    true
WHERE NOT EXISTS (
    SELECT 1 FROM flyway_schema_history WHERE version = '26'
);

-- ===============================================
-- PASSO 3: Verificar correção
-- ===============================================
-- Execute novamente para confirmar que todas as migrações
-- agora estão registradas:

SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- ===============================================
-- ALTERNATIVA: SCRIPT COMPLETO DE BASELINE
-- ===============================================
-- Se preferir fazer um baseline completo (registrar TODAS as
-- migrações de uma vez), descomente e execute o bloco abaixo:

/*
-- Este comando marca todas as migrações até a V26 como aplicadas
-- Use apenas se você tem CERTEZA de que todas as tabelas existem

DELETE FROM flyway_schema_history WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26');

INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES
(1, '1', 'create legal processes table', 'SQL', 'V1__create_legal_processes_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(2, '2', 'add document fields to event', 'SQL', 'V2__add_document_fields_to_event.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(3, '3', 'create notifications table', 'SQL', 'V3__create_notifications_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(4, '4', 'fix user created at', 'SQL', 'V4__fix_user_created_at.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(5, '5', 'fix vehicle nullable', 'SQL', 'V5__fix_vehicle_nullable.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(6, '6', 'migrate event status to new flow', 'SQL', 'V6__migrate_event_status_to_new_flow.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(7, '7', 'add status to legal processes', 'SQL', 'V7__add_status_to_legal_processes.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(8, '8', 'add process type to legal processes', 'SQL', 'V8__add_process_type_to_legal_processes.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(9, '9', 'update legal process status check', 'SQL', 'V9__update_legal_process_status_check.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(10, '10', 'add source event snapshot to legal processes', 'SQL', 'V10__add_source_event_snapshot_to_legal_processes.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(11, '11', 'expand legal process status values', 'SQL', 'V11__expand_legal_process_status_values.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(12, '12', 'sync legal process type and status', 'SQL', 'V12__sync_legal_process_type_and_status.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(13, '13', 'fix legal process status constraint', 'SQL', 'V13__fix_legal_process_status_constraint.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(14, '14', 'create vistoria table', 'SQL', 'V14__create_vistoria_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(15, '15', 'create vistoria foto table', 'SQL', 'V15__create_vistoria_foto_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(16, '16', 'add completion observation to demands', 'SQL', 'V16__add_completion_observation_to_demands.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(17, '17', 'create bill to pay table', 'SQL', 'V17__create_bill_to_pay_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(18, '18', 'create agreements table', 'SQL', 'V18__create_agreements_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(19, '19', 'add pdf columns to billing tables', 'SQL', 'V19__add_pdf_columns_to_billing_tables.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(20, '20', 'create fiscal document table', 'SQL', 'V20__create_fiscal_document_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(21, '21', 'add placa to fiscal document', 'SQL', 'V21__add_placa_to_fiscal_document.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(22, '22', 'add cnpj to partner', 'SQL', 'V22__add_cnpj_to_partner.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(23, '23', 'create nfe config table', 'SQL', 'V23__create_nfe_config_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(24, '24', 'create incoming invoice table', 'SQL', 'V24__create_incoming_invoice_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(25, '25', 'create sales table', 'SQL', 'V25__create_sales_table.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true),
(26, '26', 'update sales status to affiliation funnel', 'SQL', 'V26__update_sales_status_to_affiliation_funnel.sql', -1, CURRENT_USER, CURRENT_TIMESTAMP, 0, true);
*/

-- ===============================================
-- FIM DO SCRIPT
-- ===============================================
