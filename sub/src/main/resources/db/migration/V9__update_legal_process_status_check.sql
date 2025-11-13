-- Realinha a constraint de status para abranger os fluxos de Rastreador, Fidelidade e Terceiros
-- sem necessidade de atualizar a lista a cada novo estágio dos dois primeiros grupos
ALTER TABLE legal_processes
    DROP CONSTRAINT IF EXISTS legal_processes_status_check;

ALTER TABLE legal_processes
    ADD CONSTRAINT legal_processes_status_check CHECK (
        status ~ '^RASTREADOR_'
        OR status ~ '^FIDELIDADE_'
        OR status IN (
            'EM_ABERTO_7_0',
            'EM_CONTATO_7_1',
            'PROCESSO_JUDICIAL_7_2',
            'ACORDO_ASSINADO_7_3'
        )
    );
