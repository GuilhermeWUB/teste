-- Adiciona campos de conclusão de venda
ALTER TABLE sales ADD COLUMN IF NOT EXISTS valor_venda DOUBLE PRECISION;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS data_conclusao TIMESTAMP;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS concluida BOOLEAN DEFAULT false;

-- Adiciona índice para otimizar consultas de vendas concluídas
CREATE INDEX IF NOT EXISTS idx_sales_concluida ON sales(concluida);
CREATE INDEX IF NOT EXISTS idx_sales_data_conclusao ON sales(data_conclusao);

-- Atualiza vendas com status FILIACAO_CONCRETIZADAS para marcar como concluídas
UPDATE sales SET concluida = true WHERE status = 'FILIACAO_CONCRETIZADAS' AND concluida = false;
