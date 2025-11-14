-- Criar tabela para armazenar fotos de vistoria (sem limite)
CREATE TABLE vistoria_foto (
    id BIGSERIAL PRIMARY KEY,
    vistoria_id BIGINT NOT NULL,
    foto_path VARCHAR(500) NOT NULL,
    ordem INTEGER NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    CONSTRAINT fk_vistoria_foto_vistoria FOREIGN KEY (vistoria_id) REFERENCES vistoria(id) ON DELETE CASCADE
);

CREATE INDEX idx_vistoria_foto_vistoria_id ON vistoria_foto(vistoria_id);

-- Migrar dados existentes das 10 colunas de foto para a nova tabela
INSERT INTO vistoria_foto (vistoria_id, foto_path, ordem, data_criacao)
SELECT id, foto1_path, 1, data_criacao FROM vistoria WHERE foto1_path IS NOT NULL AND foto1_path != ''
UNION ALL
SELECT id, foto2_path, 2, data_criacao FROM vistoria WHERE foto2_path IS NOT NULL AND foto2_path != ''
UNION ALL
SELECT id, foto3_path, 3, data_criacao FROM vistoria WHERE foto3_path IS NOT NULL AND foto3_path != ''
UNION ALL
SELECT id, foto4_path, 4, data_criacao FROM vistoria WHERE foto4_path IS NOT NULL AND foto4_path != ''
UNION ALL
SELECT id, foto5_path, 5, data_criacao FROM vistoria WHERE foto5_path IS NOT NULL AND foto5_path != ''
UNION ALL
SELECT id, foto6_path, 6, data_criacao FROM vistoria WHERE foto6_path IS NOT NULL AND foto6_path != ''
UNION ALL
SELECT id, foto7_path, 7, data_criacao FROM vistoria WHERE foto7_path IS NOT NULL AND foto7_path != ''
UNION ALL
SELECT id, foto8_path, 8, data_criacao FROM vistoria WHERE foto8_path IS NOT NULL AND foto8_path != ''
UNION ALL
SELECT id, foto9_path, 9, data_criacao FROM vistoria WHERE foto9_path IS NOT NULL AND foto9_path != ''
UNION ALL
SELECT id, foto10_path, 10, data_criacao FROM vistoria WHERE foto10_path IS NOT NULL AND foto10_path != '';

-- Remover as 10 colunas antigas de foto da tabela vistoria
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto1_path;
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto2_path;
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto3_path;
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto4_path;
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto5_path;
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto6_path;
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto7_path;
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto8_path;
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto9_path;
ALTER TABLE vistoria DROP COLUMN IF EXISTS foto10_path;
