-- Adiciona campos para indicar se há terceiro envolvido e documentos de terceiros
ALTER TABLE event ADD COLUMN IF NOT EXISTS terceiro_envolvido BOOLEAN DEFAULT FALSE;
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_terceiro_cnh_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_terceiro_crlv_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_terceiro_outros_path VARCHAR(500);
