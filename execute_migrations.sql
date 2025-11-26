-- Script para executar as migrations manualmente
-- Execute este script no seu banco de dados PostgreSQL

-- Migration V27: Adiciona campos de conclusão de venda
ALTER TABLE sales ADD COLUMN IF NOT EXISTS valor_venda DOUBLE PRECISION;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS data_conclusao TIMESTAMP;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS concluida BOOLEAN DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_sales_concluida ON sales(concluida);
CREATE INDEX IF NOT EXISTS idx_sales_data_conclusao ON sales(data_conclusao);

UPDATE sales SET concluida = true WHERE status = 'FILIACAO_CONCRETIZADAS' AND concluida = false;

-- Migration V28: Cria tabela de atividades CRM
CREATE TABLE IF NOT EXISTS crm_activities (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    tipo VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    prioridade VARCHAR(50),
    sale_id BIGINT,
    contato_nome VARCHAR(255),
    contato_email VARCHAR(255),
    contato_telefone VARCHAR(50),
    data_agendada TIMESTAMP,
    data_realizada TIMESTAMP,
    responsavel VARCHAR(255),
    resultado TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_crm_activities_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_crm_activities_status ON crm_activities(status);
CREATE INDEX IF NOT EXISTS idx_crm_activities_tipo ON crm_activities(tipo);
CREATE INDEX IF NOT EXISTS idx_crm_activities_sale_id ON crm_activities(sale_id);
CREATE INDEX IF NOT EXISTS idx_crm_activities_data_agendada ON crm_activities(data_agendada);
CREATE INDEX IF NOT EXISTS idx_crm_activities_responsavel ON crm_activities(responsavel);
CREATE INDEX IF NOT EXISTS idx_crm_activities_created_at ON crm_activities(created_at DESC);

-- Registrar as migrations no Flyway (se necessário)
-- INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
-- VALUES (27, '27', 'add sale completion fields', 'SQL', 'V27__add_sale_completion_fields.sql', 0, 'postgres', 1, true);
-- INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
-- VALUES (28, '28', 'create crm activities table', 'SQL', 'V28__create_crm_activities_table.sql', 0, 'postgres', 1, true);
