-- Migration V11: Create regionais table
-- Table to store company regional offices

CREATE TABLE regionais (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_regionais_active ON regionais(active);
CREATE INDEX idx_regionais_code ON regionais(code);
CREATE INDEX idx_regionais_name ON regionais(name);

-- Add some example regionais
INSERT INTO regionais (name, code, description, active) VALUES
('Regional Sul', 'SUL', 'Responsável pelos estados do Sul do Brasil', true),
('Regional Sudeste', 'SUDESTE', 'Responsável pelos estados do Sudeste do Brasil', true),
('Regional Nordeste', 'NORDESTE', 'Responsável pelos estados do Nordeste do Brasil', true),
('Regional Norte', 'NORTE', 'Responsável pelos estados do Norte do Brasil', true),
('Regional Centro-Oeste', 'CENTRO_OESTE', 'Responsável pelos estados do Centro-Oeste do Brasil', true);
