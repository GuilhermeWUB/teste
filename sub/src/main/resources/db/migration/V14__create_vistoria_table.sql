-- Tabela para armazenar as vistorias com fotos do acidente
-- Uma vistoria está vinculada a um evento/comunicado
CREATE TABLE vistoria (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    foto1_path VARCHAR(500),
    foto2_path VARCHAR(500),
    foto3_path VARCHAR(500),
    foto4_path VARCHAR(500),
    foto5_path VARCHAR(500),
    foto6_path VARCHAR(500),
    foto7_path VARCHAR(500),
    foto8_path VARCHAR(500),
    foto9_path VARCHAR(500),
    foto10_path VARCHAR(500),
    observacoes TEXT,
    data_criacao TIMESTAMP NOT NULL,
    usuario_criacao VARCHAR(255),

    CONSTRAINT fk_vistoria_event
        FOREIGN KEY (event_id)
        REFERENCES event(id)
        ON DELETE CASCADE
);

-- Índice para melhorar performance nas consultas por evento
CREATE INDEX idx_vistoria_event_id ON vistoria(event_id);
