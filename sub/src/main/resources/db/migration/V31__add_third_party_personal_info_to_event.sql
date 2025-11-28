-- Adiciona campos de informações pessoais do terceiro envolvido
ALTER TABLE event ADD COLUMN IF NOT EXISTS terceiro_nome VARCHAR(200);
ALTER TABLE event ADD COLUMN IF NOT EXISTS terceiro_cpf VARCHAR(14);
ALTER TABLE event ADD COLUMN IF NOT EXISTS terceiro_telefone VARCHAR(20);
