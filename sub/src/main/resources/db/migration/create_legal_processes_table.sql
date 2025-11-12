-- Cria a tabela para armazenar os processos jurídicos cadastrados via kanban
CREATE TABLE IF NOT EXISTS legal_processes (
    id BIGSERIAL PRIMARY KEY,
    autor VARCHAR(255) NOT NULL,
    reu VARCHAR(255) NOT NULL,
    materia VARCHAR(255) NOT NULL,
    numero_processo VARCHAR(100) NOT NULL UNIQUE,
    valor_causa NUMERIC(19, 2) NOT NULL,
    pedidos TEXT NOT NULL
);

-- Garante a existência de um índice para buscas rápidas pelo número do processo
CREATE UNIQUE INDEX IF NOT EXISTS ux_legal_processes_numero_processo
    ON legal_processes (numero_processo);
