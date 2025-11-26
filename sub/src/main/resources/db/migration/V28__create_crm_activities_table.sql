-- Cria tabela de atividades CRM
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

-- Cria índices para otimizar consultas
CREATE INDEX IF NOT EXISTS idx_crm_activities_status ON crm_activities(status);
CREATE INDEX IF NOT EXISTS idx_crm_activities_tipo ON crm_activities(tipo);
CREATE INDEX IF NOT EXISTS idx_crm_activities_sale_id ON crm_activities(sale_id);
CREATE INDEX IF NOT EXISTS idx_crm_activities_data_agendada ON crm_activities(data_agendada);
CREATE INDEX IF NOT EXISTS idx_crm_activities_responsavel ON crm_activities(responsavel);
CREATE INDEX IF NOT EXISTS idx_crm_activities_created_at ON crm_activities(created_at DESC);
