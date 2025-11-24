-- Atualiza os status de vendas para refletir o funil de filiacao

-- Remove a constraint antiga
ALTER TABLE sales DROP CONSTRAINT IF EXISTS chk_sale_status;

-- Atualiza os status existentes para os novos valores
UPDATE sales SET status = 'COTACOES_RECEBIDAS' WHERE status = 'NOVO_LEAD';
UPDATE sales SET status = 'EM_NEGOCIACAO' WHERE status IN ('CONTATO_INICIAL', 'PROPOSTA_ENVIADA', 'NEGOCIACAO');
UPDATE sales SET status = 'FILIACAO_CONCRETIZADAS' WHERE status = 'FECHADO';

-- Adiciona a nova constraint com os status corretos
ALTER TABLE sales ADD CONSTRAINT chk_sale_status CHECK (status IN ('COTACOES_RECEBIDAS', 'EM_NEGOCIACAO', 'VISTORIAS', 'LIBERADAS_PARA_CADASTRO', 'FILIACAO_CONCRETIZADAS'));

-- Atualiza o default para novos registros
ALTER TABLE sales ALTER COLUMN status SET DEFAULT 'COTACOES_RECEBIDAS';
