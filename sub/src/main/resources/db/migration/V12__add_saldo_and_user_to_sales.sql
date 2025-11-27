-- Migration V12: Add saldo to app_users and user_id to sales

-- Add saldo column to app_users
ALTER TABLE app_users
ADD COLUMN saldo DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

-- Add user_id column to sales (to track which user made the sale)
ALTER TABLE sales
ADD COLUMN user_id BIGINT;

-- Add foreign key constraint
ALTER TABLE sales
ADD CONSTRAINT fk_sales_user
FOREIGN KEY (user_id) REFERENCES app_users(id);

-- Create index for better query performance
CREATE INDEX idx_sales_user_id ON sales(user_id);

COMMENT ON COLUMN app_users.saldo IS 'Saldo acumulado de vendas concluídas do usuário';
COMMENT ON COLUMN sales.user_id IS 'ID do usuário que realizou/é responsável pela venda';
