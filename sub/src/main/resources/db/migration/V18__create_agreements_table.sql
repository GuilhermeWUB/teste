-- Create agreements table for Kanban board
CREATE TABLE agreements (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,
    parte_envolvida VARCHAR(255) NOT NULL,
    valor DECIMAL(15, 2) NOT NULL,
    data_vencimento DATE,
    data_pagamento DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE',
    observacoes VARCHAR(2000),
    numero_parcelas INTEGER,
    parcela_atual INTEGER,
    numero_processo VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_agreement_status CHECK (status IN ('PENDENTE', 'EM_NEGOCIACAO', 'APROVADO', 'PAGO', 'CANCELADO', 'VENCIDO'))
);

-- Create indexes for better query performance
CREATE INDEX idx_agreements_status ON agreements(status);
CREATE INDEX idx_agreements_data_vencimento ON agreements(data_vencimento);
CREATE INDEX idx_agreements_numero_processo ON agreements(numero_processo);
