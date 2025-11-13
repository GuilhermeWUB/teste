-- Adiciona coluna para armazenar o tipo de cobrança associado ao processo
ALTER TABLE legal_processes
    ADD COLUMN IF NOT EXISTS process_type VARCHAR(30) NOT NULL DEFAULT 'TERCEIROS';

-- Atualiza o tipo de acordo com o status atual do processo
UPDATE legal_processes
SET process_type = 'RASTREADOR'
WHERE status IN (
    'RASTREADOR_EM_ABERTO',
    'RASTREADOR_EM_CONTATO',
    'RASTREADOR_ACORDO_ASSINADO',
    'RASTREADOR_DEVOLVIDO',
    'RASTREADOR_REATIVACAO'
);

UPDATE legal_processes
SET process_type = 'FIDELIDADE'
WHERE status IN (
    'FIDELIDADE_EM_ABERTO',
    'FIDELIDADE_EM_CONTATO',
    'FIDELIDADE_ACORDO_ASSINADO',
    'FIDELIDADE_REATIVACAO'
);

UPDATE legal_processes
SET process_type = 'TERCEIROS'
WHERE process_type IS NULL
   OR process_type NOT IN ('RASTREADOR', 'FIDELIDADE', 'TERCEIROS');

-- Índice auxiliar para consultas por tipo de cobrança
CREATE INDEX IF NOT EXISTS idx_legal_processes_process_type
    ON legal_processes (process_type);
