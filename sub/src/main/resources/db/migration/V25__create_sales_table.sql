-- Tabela para gerenciar funil de filiacao do CRM
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    cooperativa VARCHAR(255),
    tipo_veiculo VARCHAR(255),
    placa VARCHAR(50),
    marca VARCHAR(255),
    ano_modelo VARCHAR(20),
    modelo VARCHAR(255),
    nome_contato VARCHAR(255),
    email VARCHAR(255),
    celular VARCHAR(20),
    estado VARCHAR(2),
    cidade VARCHAR(255),
    origem_lead VARCHAR(255),
    veiculo_trabalho BOOLEAN DEFAULT FALSE,
    enviar_cotacao BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'COTACOES_RECEBIDAS',
    observacoes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sale_status CHECK (status IN ('COTACOES_RECEBIDAS', 'EM_NEGOCIACAO', 'VISTORIAS', 'LIBERADAS_PARA_CADASTRO', 'FILIACAO_CONCRETIZADAS'))
);

-- Indice para melhorar performance de buscas por status
CREATE INDEX idx_sales_status ON sales(status);

-- Indice para melhorar performance de buscas por nome de contato
CREATE INDEX idx_sales_nome_contato ON sales(nome_contato);

-- Indice para ordenacao por data de criacao
CREATE INDEX idx_sales_created_at ON sales(created_at DESC);
