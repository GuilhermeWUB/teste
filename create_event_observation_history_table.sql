-- Cria tabela para histórico de alterações de observações de eventos
CREATE TABLE IF NOT EXISTS event_observation_history (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    previous_observation TEXT,
    new_observation TEXT,
    modified_by VARCHAR(100),
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_observation_history_event
        FOREIGN KEY (event_id)
        REFERENCES event(id)
        ON DELETE CASCADE
);

-- Cria índice para melhor performance nas consultas
CREATE INDEX IF NOT EXISTS idx_event_observation_history_event_id
    ON event_observation_history(event_id);

CREATE INDEX IF NOT EXISTS idx_event_observation_history_modified_at
    ON event_observation_history(modified_at DESC);

-- Comentários para documentação
COMMENT ON TABLE event_observation_history IS 'Histórico de alterações do campo observações dos eventos';
COMMENT ON COLUMN event_observation_history.event_id IS 'ID do evento associado';
COMMENT ON COLUMN event_observation_history.previous_observation IS 'Valor anterior do campo observações';
COMMENT ON COLUMN event_observation_history.new_observation IS 'Novo valor do campo observações';
COMMENT ON COLUMN event_observation_history.modified_by IS 'Username de quem fez a alteração';
COMMENT ON COLUMN event_observation_history.modified_at IS 'Data e hora da alteração';
