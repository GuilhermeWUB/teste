-- Adiciona campo CNPJ na tabela Partner para suportar fornecedores pessoa jurídica
ALTER TABLE partner ADD COLUMN cnpj VARCHAR(14);

-- Cria índice para melhorar performance em buscas por CNPJ
CREATE INDEX idx_partner_cnpj ON partner(cnpj);
