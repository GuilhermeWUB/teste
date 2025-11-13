-- Adiciona coluna de status para suportar o fluxo do Kanban de processos jurídicos
ALTER TABLE legal_processes
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'EM_ABERTO_7_0';

-- Cria índice para otimizar consultas por status
CREATE INDEX IF NOT EXISTS idx_legal_processes_status
    ON legal_processes (status);

-- Atualiza processos existentes para o status padrão
UPDATE legal_processes
SET status = 'EM_ABERTO_7_0'
WHERE status IS NULL;
