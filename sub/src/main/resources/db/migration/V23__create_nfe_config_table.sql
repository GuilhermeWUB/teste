-- Cria tabela para armazenar configurações de integração com a SEFAZ
CREATE TABLE nfe_config (
    id BIGSERIAL PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    certificado_path VARCHAR(500) NOT NULL,
    certificado_senha VARCHAR(255) NOT NULL,
    ultimo_nsu VARCHAR(20) NOT NULL DEFAULT '0',
    ambiente VARCHAR(20) NOT NULL DEFAULT 'HOMOLOGACAO',
    uf VARCHAR(2) DEFAULT 'SP',
    ativo BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT chk_ambiente CHECK (ambiente IN ('HOMOLOGACAO', 'PRODUCAO'))
);

-- Comentários nas colunas
COMMENT ON TABLE nfe_config IS 'Configurações para integração com SEFAZ - Consulta de NFe';
COMMENT ON COLUMN nfe_config.cnpj IS 'CNPJ da empresa para consulta na SEFAZ';
COMMENT ON COLUMN nfe_config.certificado_path IS 'Caminho do arquivo .pfx no disco';
COMMENT ON COLUMN nfe_config.certificado_senha IS 'Senha do certificado digital';
COMMENT ON COLUMN nfe_config.ultimo_nsu IS 'Último NSU consultado (para paginação)';
COMMENT ON COLUMN nfe_config.ambiente IS 'Ambiente SEFAZ: HOMOLOGACAO ou PRODUCAO';
COMMENT ON COLUMN nfe_config.uf IS 'UF do emitente';
COMMENT ON COLUMN nfe_config.ativo IS 'Flag para habilitar/desabilitar consulta automática';
