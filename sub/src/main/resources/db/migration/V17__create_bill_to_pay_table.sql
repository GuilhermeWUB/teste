-- Criação da tabela de boletos a pagar (contas a pagar / saídas financeiras)
CREATE TABLE bill_to_pay (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    valor_pago DECIMAL(10, 2),
    status INTEGER NOT NULL DEFAULT 0,
    fornecedor VARCHAR(255),
    categoria VARCHAR(100),
    observacao TEXT,
    numero_documento VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para melhor performance
CREATE INDEX idx_bill_to_pay_status ON bill_to_pay(status);
CREATE INDEX idx_bill_to_pay_vencimento ON bill_to_pay(data_vencimento);
CREATE INDEX idx_bill_to_pay_pagamento ON bill_to_pay(data_pagamento);

-- Comentários na tabela
COMMENT ON TABLE bill_to_pay IS 'Tabela para armazenar boletos/contas a pagar (saídas financeiras)';
COMMENT ON COLUMN bill_to_pay.status IS '0 = Pendente, 1 = Pago';
