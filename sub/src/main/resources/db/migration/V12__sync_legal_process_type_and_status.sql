-- Garantir que os processos jurídicos aceitam os fluxos de Rastreador e Fidelidade
-- e sincronizar dados existentes com as novas regras de tipo/status
ALTER TABLE legal_processes
    ADD COLUMN IF NOT EXISTS process_type VARCHAR(30) NOT NULL DEFAULT 'TERCEIROS';

-- Ajusta o tipo de cobrança de acordo com o status já registrado
UPDATE legal_processes
SET process_type = 'RASTREADOR'
WHERE status LIKE 'RASTREADOR_%';

UPDATE legal_processes
SET process_type = 'FIDELIDADE'
WHERE status LIKE 'FIDELIDADE_%';

UPDATE legal_processes
SET process_type = 'TERCEIROS'
WHERE process_type IS NULL
   OR process_type NOT IN ('RASTREADOR', 'FIDELIDADE', 'TERCEIROS');

ALTER TABLE legal_processes
    DROP CONSTRAINT IF EXISTS legal_processes_process_type_check;

ALTER TABLE legal_processes
    ADD CONSTRAINT legal_processes_process_type_check CHECK (
        process_type IN ('RASTREADOR', 'FIDELIDADE', 'TERCEIROS')
    );

ALTER TABLE legal_processes
    DROP CONSTRAINT IF EXISTS legal_processes_status_check;

ALTER TABLE legal_processes
    ADD CONSTRAINT legal_processes_status_check CHECK (
        status LIKE 'RASTREADOR_%'
        OR status LIKE 'FIDELIDADE_%'
        OR status IN (
            'EM_ABERTO_7_0',
            'EM_CONTATO_7_1',
            'PROCESSO_JUDICIAL_7_2',
            'ACORDO_ASSINADO_7_3'
        )
    );

-- Garante que status e tipo estejam alinhados após atualizar a constraint
UPDATE legal_processes
SET status = 'RASTREADOR_EM_ABERTO'
WHERE process_type = 'RASTREADOR'
  AND status NOT LIKE 'RASTREADOR_%';

UPDATE legal_processes
SET status = 'FIDELIDADE_EM_ABERTO'
WHERE process_type = 'FIDELIDADE'
  AND status NOT LIKE 'FIDELIDADE_%';

UPDATE legal_processes
SET status = 'EM_ABERTO_7_0'
WHERE process_type = 'TERCEIROS'
  AND status NOT IN (
      'EM_ABERTO_7_0',
      'EM_CONTATO_7_1',
      'PROCESSO_JUDICIAL_7_2',
      'ACORDO_ASSINADO_7_3'
  );
