-- Migration: Criação da tabela de notificações
-- Data: 2025-11-11
-- Descrição: Cria a estrutura completa para o sistema de notificações

-- Criação da tabela notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    action_url VARCHAR(500),
    related_entity_id BIGINT,
    related_entity_type VARCHAR(100),
    priority VARCHAR(20),

    -- Foreign key para user_account
    CONSTRAINT fk_notification_recipient
        FOREIGN KEY (recipient_id)
        REFERENCES user_account(id)
        ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_notification_type
        CHECK (type IN ('EVENT', 'DEMAND', 'PAYMENT', 'BANK_SLIP', 'COMUNICADO',
                        'PARTNER', 'VEHICLE', 'SYSTEM', 'ALERT', 'INFO')),

    CONSTRAINT chk_notification_status
        CHECK (status IN ('UNREAD', 'READ', 'ARCHIVED')),

    CONSTRAINT chk_notification_priority
        CHECK (priority IS NULL OR priority IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE'))
);

-- Índices para otimização de consultas
CREATE INDEX idx_recipient_status ON notifications(recipient_id, status);
CREATE INDEX idx_recipient_created ON notifications(recipient_id, created_at DESC);
CREATE INDEX idx_type_status ON notifications(type, status);
CREATE INDEX idx_related_entity ON notifications(related_entity_type, related_entity_id);
CREATE INDEX idx_created_at ON notifications(created_at DESC);

-- Comentários nas colunas
COMMENT ON TABLE notifications IS 'Tabela de notificações do sistema';
COMMENT ON COLUMN notifications.recipient_id IS 'ID do usuário destinatário da notificação';
COMMENT ON COLUMN notifications.title IS 'Título da notificação';
COMMENT ON COLUMN notifications.message IS 'Mensagem/conteúdo da notificação';
COMMENT ON COLUMN notifications.type IS 'Tipo da notificação (EVENT, DEMAND, PAYMENT, etc)';
COMMENT ON COLUMN notifications.status IS 'Status da notificação (UNREAD, READ, ARCHIVED)';
COMMENT ON COLUMN notifications.created_at IS 'Data e hora de criação';
COMMENT ON COLUMN notifications.read_at IS 'Data e hora em que foi lida';
COMMENT ON COLUMN notifications.action_url IS 'URL para ação relacionada à notificação';
COMMENT ON COLUMN notifications.related_entity_id IS 'ID da entidade relacionada (Event, Demand, etc)';
COMMENT ON COLUMN notifications.related_entity_type IS 'Tipo da entidade relacionada';
COMMENT ON COLUMN notifications.priority IS 'Prioridade da notificação';
