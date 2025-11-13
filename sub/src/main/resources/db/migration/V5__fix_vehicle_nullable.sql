-- Remove a constraint NOT NULL da coluna vehicle_id na tabela event
-- Isso permite que eventos sejam criados sem um veículo associado
-- O campo placaManual pode ser usado como alternativa

ALTER TABLE event ALTER COLUMN vehicle_id DROP NOT NULL;
