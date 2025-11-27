-- Cria tabela de saques (withdrawals)
CREATE TABLE IF NOT EXISTS withdrawals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_date TIMESTAMP,
    pix_key VARCHAR(100),
    observation VARCHAR(500),
    CONSTRAINT fk_withdrawals_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT chk_withdrawals_status CHECK (status IN ('PENDENTE', 'APROVADO', 'REJEITADO', 'CONCLUIDO'))
);

-- Cria índices para otimizar consultas
CREATE INDEX IF NOT EXISTS idx_withdrawals_user_id ON withdrawals(user_id);
CREATE INDEX IF NOT EXISTS idx_withdrawals_status ON withdrawals(status);
CREATE INDEX IF NOT EXISTS idx_withdrawals_request_date ON withdrawals(request_date DESC);

-- Comentários para documentação
COMMENT ON TABLE withdrawals IS 'Tabela de solicitações de saque de usuários';
COMMENT ON COLUMN withdrawals.user_id IS 'ID do usuário que solicitou o saque';
COMMENT ON COLUMN withdrawals.amount IS 'Valor solicitado para saque';
COMMENT ON COLUMN withdrawals.status IS 'Status do saque: PENDENTE, APROVADO, REJEITADO, CONCLUIDO';
COMMENT ON COLUMN withdrawals.request_date IS 'Data da solicitação do saque';
COMMENT ON COLUMN withdrawals.completed_date IS 'Data de conclusão/rejeição do saque';
COMMENT ON COLUMN withdrawals.pix_key IS 'Chave PIX para transferência';
COMMENT ON COLUMN withdrawals.observation IS 'Observações sobre o saque';
