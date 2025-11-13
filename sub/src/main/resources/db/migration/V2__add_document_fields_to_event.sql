-- Adiciona campos para armazenar documentos anexados aos eventos
-- Executar este script no banco de dados PostgreSQL

ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_crlv_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_cnh_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_bo_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_comprovante_residencia_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_termo_abertura_path VARCHAR(500);

COMMENT ON COLUMN event.doc_crlv_path IS 'Caminho do arquivo CRLV (Certificado de Registro e Licenciamento de Veículo)';
COMMENT ON COLUMN event.doc_cnh_path IS 'Caminho do arquivo CNH (Carteira Nacional de Habilitação)';
COMMENT ON COLUMN event.doc_bo_path IS 'Caminho do arquivo B.O. (Boletim de Ocorrência)';
COMMENT ON COLUMN event.doc_comprovante_residencia_path IS 'Caminho do arquivo Comprovante de residência atualizado';
COMMENT ON COLUMN event.doc_termo_abertura_path IS 'Caminho do arquivo Termo de abertura assinado';
