-- Cria tabela para armazenar as notas fiscais de entrada (caixa de entrada)
CREATE TABLE incoming_invoice (
    id BIGSERIAL PRIMARY KEY,
    chave_acesso VARCHAR(44) NOT NULL UNIQUE,
    numero_nota VARCHAR(20) NOT NULL,
    cnpj_emitente VARCHAR(14) NOT NULL,
    nome_emitente VARCHAR(255) NOT NULL,
    valor_total DECIMAL(15,2) NOT NULL,
    data_emissao TIMESTAMP NOT NULL,
    xml_content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    bill_to_pay_id BIGINT,
    observacoes TEXT,

    CONSTRAINT chk_status CHECK (status IN ('PENDENTE', 'PROCESSADA', 'IGNORADA'))
);

-- Índices para melhorar performance
CREATE INDEX idx_incoming_invoice_status ON incoming_invoice(status);
CREATE INDEX idx_incoming_invoice_cnpj ON incoming_invoice(cnpj_emitente);
CREATE INDEX idx_incoming_invoice_imported ON incoming_invoice(imported_at);
CREATE INDEX idx_incoming_invoice_chave ON incoming_invoice(chave_acesso);

-- Comentários nas colunas
COMMENT ON TABLE incoming_invoice IS 'Caixa de entrada de notas fiscais importadas da SEFAZ';
COMMENT ON COLUMN incoming_invoice.chave_acesso IS 'Chave de acesso da NFe (44 dígitos)';
COMMENT ON COLUMN incoming_invoice.numero_nota IS 'Número da nota fiscal';
COMMENT ON COLUMN incoming_invoice.cnpj_emitente IS 'CNPJ do emissor (fornecedor)';
COMMENT ON COLUMN incoming_invoice.nome_emitente IS 'Razão social do emissor';
COMMENT ON COLUMN incoming_invoice.valor_total IS 'Valor total da nota';
COMMENT ON COLUMN incoming_invoice.data_emissao IS 'Data e hora de emissão da nota';
COMMENT ON COLUMN incoming_invoice.xml_content IS 'XML completo da nota para processamento';
COMMENT ON COLUMN incoming_invoice.status IS 'Status: PENDENTE, PROCESSADA ou IGNORADA';
COMMENT ON COLUMN incoming_invoice.imported_at IS 'Data/hora de importação';
COMMENT ON COLUMN incoming_invoice.processed_at IS 'Data/hora de processamento';
COMMENT ON COLUMN incoming_invoice.bill_to_pay_id IS 'ID da conta a pagar gerada (se processada)';
COMMENT ON COLUMN incoming_invoice.observacoes IS 'Observações sobre processamento';
