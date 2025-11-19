CREATE TABLE IF NOT EXISTS fiscal_document (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    numero_nota VARCHAR(120),
    valor NUMERIC(19, 2),
    data_emissao DATE,
    pdf_path VARCHAR(500) NOT NULL,
    data_upload TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);
