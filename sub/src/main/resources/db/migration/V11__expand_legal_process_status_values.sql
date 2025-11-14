-- Expande a constraint de status para abranger todos os valores suportados pela aplicação
ALTER TABLE legal_processes
    DROP CONSTRAINT IF EXISTS legal_processes_status_check;

ALTER TABLE legal_processes
    ADD CONSTRAINT legal_processes_status_check CHECK (
        status IN (
            'RASTREADOR_EM_ABERTO',
            'RASTREADOR_EM_CONTATO',
            'RASTREADOR_ACORDO_ASSINADO',
            'RASTREADOR_DEVOLVIDO',
            'RASTREADOR_REATIVACAO',
            'FIDELIDADE_EM_ABERTO',
            'FIDELIDADE_EM_CONTATO',
            'FIDELIDADE_ACORDO_ASSINADO',
            'FIDELIDADE_REATIVACAO',
            'EM_ABERTO_7_0',
            'EM_CONTATO_7_1',
            'PROCESSO_JUDICIAL_7_2',
            'ACORDO_ASSINADO_7_3'
        )
    );
