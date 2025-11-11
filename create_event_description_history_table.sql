-- Cria tabela para histórico de alterações de descrições de eventos
CREATE TABLE IF NOT EXISTS event_description_history (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    previous_description TEXT,
    new_description TEXT,
    modified_by VARCHAR(100),
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_description_history_event
        FOREIGN KEY (event_id)
        REFERENCES event(id)
        ON DELETE CASCADE
);

-- Cria índice para melhor performance nas consultas
CREATE INDEX IF NOT EXISTS idx_event_description_history_event_id
    ON event_description_history(event_id);

CREATE INDEX IF NOT EXISTS idx_event_description_history_modified_at
    ON event_description_history(modified_at DESC);

-- Comentários para documentação
COMMENT ON TABLE event_description_history IS 'Histórico de alterações do campo descrição dos eventos';
COMMENT ON COLUMN event_description_history.event_id IS 'ID do evento associado';
COMMENT ON COLUMN event_description_history.previous_description IS 'Valor anterior do campo descrição';
COMMENT ON COLUMN event_description_history.new_description IS 'Novo valor do campo descrição';
COMMENT ON COLUMN event_description_history.modified_by IS 'Username de quem fez a alteração';
COMMENT ON COLUMN event_description_history.modified_at IS 'Data e hora da alteração';
