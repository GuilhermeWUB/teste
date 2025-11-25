-- ===============================================
-- SCRIPT COMPLETO DE CRIAÇÃO DO BANCO DE DADOS
-- ===============================================
-- Este script consolida todas as migrações Flyway (V1 até V26)
-- em um único arquivo para criação do banco de dados do zero.
--
-- Projeto: NECSUS Spring
-- Database: PostgreSQL
-- Data de geração: 2025-11-25
--
-- IMPORTANTE:
-- - Este script NÃO inclui as tabelas base gerenciadas pelo JPA/Hibernate
--   (event, partner, vehicle, user_account/app_users, demands, bank_slips, etc.)
-- - Essas tabelas são criadas automaticamente pelo Hibernate no primeiro start
-- - Este script contém APENAS as migrações Flyway aplicadas manualmente
--
-- COMO USAR:
-- 1. Crie um banco de dados vazio PostgreSQL
-- 2. Execute este script para criar a estrutura base
-- 3. Inicie a aplicação Spring Boot (Hibernate criará as tabelas JPA)
-- 4. As migrações Flyway serão aplicadas automaticamente
--
-- ALTERNATIVA - Banco do Zero:
-- Se você quer um banco completamente novo:
-- 1. Execute este script
-- 2. Configure spring.jpa.hibernate.ddl-auto=update ou create
-- 3. Inicie a aplicação
-- ===============================================

-- ===============================================
-- V1: Criar tabela de processos jurídicos
-- ===============================================
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

-- ===============================================
-- V2: Adicionar campos de documentos ao evento
-- ===============================================
-- Adiciona campos para armazenar documentos anexados aos eventos
-- NOTA: A tabela 'event' é criada pelo Hibernate
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_crlv_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_cnh_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_bo_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_comprovante_residencia_path VARCHAR(500);
ALTER TABLE event ADD COLUMN IF NOT EXISTS doc_termo_abertura_path VARCHAR(500);

COMMENT ON COLUMN event.doc_crlv_path IS 'Caminho do arquivo CRLV (Certificado de Registro e Licenciamento de Veículo)';
COMMENT ON COLUMN event.doc_cnh_path IS 'Caminho do arquivo CNH (Carteira Nacional de Habilitação)';
COMMENT ON COLUMN event.doc_bo_path IS 'Caminho do arquivo B.O. (Boletim de Ocorrência)';
COMMENT ON COLUMN event.doc_comprovante_residencia_path IS 'Caminho do arquivo Comprovante de residência atualizado';
COMMENT ON COLUMN event.doc_termo_abertura_path IS 'Caminho do arquivo Termo de abertura assinado';

-- ===============================================
-- V3: Criar tabela de notificações
-- ===============================================
-- Cria a estrutura completa para o sistema de notificações
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    action_url VARCHAR(500),
    related_entity_id BIGINT,
    related_entity_type VARCHAR(100),
    priority VARCHAR(20),

    -- Foreign key para user_account
    CONSTRAINT fk_notification_recipient
        FOREIGN KEY (recipient_id)
        REFERENCES user_account(id)
        ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_notification_type
        CHECK (type IN ('EVENT', 'DEMAND', 'PAYMENT', 'BANK_SLIP', 'COMUNICADO',
                        'PARTNER', 'VEHICLE', 'SYSTEM', 'ALERT', 'INFO')),

    CONSTRAINT chk_notification_status
        CHECK (status IN ('UNREAD', 'READ', 'ARCHIVED')),

    CONSTRAINT chk_notification_priority
        CHECK (priority IS NULL OR priority IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE'))
);

CREATE INDEX IF NOT EXISTS idx_recipient_status ON notifications(recipient_id, status);
CREATE INDEX IF NOT EXISTS idx_recipient_created ON notifications(recipient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_type_status ON notifications(type, status);
CREATE INDEX IF NOT EXISTS idx_related_entity ON notifications(related_entity_type, related_entity_id);
CREATE INDEX IF NOT EXISTS idx_created_at ON notifications(created_at DESC);

-- Comentários nas colunas
COMMENT ON TABLE notifications IS 'Tabela de notificações do sistema';
COMMENT ON COLUMN notifications.recipient_id IS 'ID do usuário destinatário da notificação';
COMMENT ON COLUMN notifications.title IS 'Título da notificação';
COMMENT ON COLUMN notifications.message IS 'Mensagem/conteúdo da notificação';
COMMENT ON COLUMN notifications.type IS 'Tipo da notificação (EVENT, DEMAND, PAYMENT, etc)';
COMMENT ON COLUMN notifications.status IS 'Status da notificação (UNREAD, READ, ARCHIVED)';
COMMENT ON COLUMN notifications.created_at IS 'Data e hora de criação';
COMMENT ON COLUMN notifications.read_at IS 'Data e hora em que foi lida';
COMMENT ON COLUMN notifications.action_url IS 'URL para ação relacionada à notificação';
COMMENT ON COLUMN notifications.related_entity_id IS 'ID da entidade relacionada (Event, Demand, etc)';
COMMENT ON COLUMN notifications.related_entity_type IS 'Tipo da entidade relacionada';
COMMENT ON COLUMN notifications.priority IS 'Prioridade da notificação';

-- ===============================================
-- V4: Corrigir campo created_at na tabela app_users
-- ===============================================
-- Corrige valores NULL na coluna created_at e adiciona proteções
UPDATE app_users
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE app_users
ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE app_users
ALTER COLUMN created_at SET NOT NULL;

-- ===============================================
-- V5: Tornar vehicle_id opcional na tabela event
-- ===============================================
-- Remove a constraint NOT NULL da coluna vehicle_id na tabela event
-- Isso permite que eventos sejam criados sem um veículo associado
ALTER TABLE event ALTER COLUMN vehicle_id DROP NOT NULL;

-- ===============================================
-- V6: Migrar status de eventos para novo fluxo
-- ===============================================
-- Remove os status antigos e prepara o banco para os novos 19 status do fluxo de sinistro
-- IMPORTANTE: Este script APAGA todos os eventos existentes

-- Limpar todos os eventos existentes
DELETE FROM event;

-- Resetar a sequência de IDs (PostgreSQL)
ALTER SEQUENCE IF EXISTS event_id_seq RESTART WITH 1;

-- ===============================================
-- V7: Adicionar coluna status aos processos jurídicos
-- ===============================================
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

-- ===============================================
-- V8: Adicionar tipo de processo aos processos jurídicos
-- ===============================================
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

-- ===============================================
-- V9: Atualizar constraint de status com regex
-- ===============================================
-- Realinha a constraint de status para abranger os fluxos de Rastreador, Fidelidade e Terceiros
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

-- ===============================================
-- V10: Adicionar campos source_event aos processos jurídicos
-- ===============================================
ALTER TABLE legal_processes
    ADD COLUMN IF NOT EXISTS source_event_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_event_snapshot TEXT;

-- ===============================================
-- V11: Expandir valores de status permitidos
-- ===============================================
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

-- ===============================================
-- V12: Sincronizar tipo e status de processos
-- ===============================================
-- Garantir que os processos jurídicos aceitam os fluxos de Rastreador e Fidelidade
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

-- ===============================================
-- V13: Corrigir constraint de status
-- ===============================================
-- Remove a constraint antiga, se existir
ALTER TABLE legal_processes
    DROP CONSTRAINT IF EXISTS legal_processes_status_check;

-- Adiciona a constraint atualizada com todos os status permitidos
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

-- ===============================================
-- V14: Criar tabela de vistoria
-- ===============================================
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

-- ===============================================
-- V15: Criar tabela de fotos de vistoria
-- ===============================================
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

-- ===============================================
-- V16: Adicionar observação de conclusão às demandas
-- ===============================================
ALTER TABLE demands
    ADD COLUMN completion_observation TEXT;

-- ===============================================
-- V17: Criar tabela de contas a pagar
-- ===============================================
-- Criação da tabela de boletos a pagar (contas a pagar / saídas financeiras)
CREATE TABLE bill_to_pay (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    valor_pago DECIMAL(10, 2),
    status INTEGER NOT NULL DEFAULT 0,
    fornecedor VARCHAR(255),
    categoria VARCHAR(100),
    observacao TEXT,
    numero_documento VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para melhor performance
CREATE INDEX idx_bill_to_pay_status ON bill_to_pay(status);
CREATE INDEX idx_bill_to_pay_vencimento ON bill_to_pay(data_vencimento);
CREATE INDEX idx_bill_to_pay_pagamento ON bill_to_pay(data_pagamento);

-- Comentários na tabela
COMMENT ON TABLE bill_to_pay IS 'Tabela para armazenar boletos/contas a pagar (saídas financeiras)';
COMMENT ON COLUMN bill_to_pay.status IS '0 = Pendente, 1 = Pago';

-- ===============================================
-- V18: Criar tabela de acordos
-- ===============================================
-- Create agreements table for Kanban board
CREATE TABLE agreements (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,
    parte_envolvida VARCHAR(255) NOT NULL,
    valor DECIMAL(15, 2) NOT NULL,
    data_vencimento DATE,
    data_pagamento DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE',
    observacoes VARCHAR(2000),
    numero_parcelas INTEGER,
    parcela_atual INTEGER,
    numero_processo VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_agreement_status CHECK (status IN ('PENDENTE', 'EM_NEGOCIACAO', 'APROVADO', 'PAGO', 'CANCELADO', 'VENCIDO'))
);

-- Create indexes for better query performance
CREATE INDEX idx_agreements_status ON agreements(status);
CREATE INDEX idx_agreements_data_vencimento ON agreements(data_vencimento);
CREATE INDEX idx_agreements_numero_processo ON agreements(numero_processo);

-- ===============================================
-- V19: Adicionar coluna PDF às tabelas financeiras
-- ===============================================
ALTER TABLE bank_slips
    ADD COLUMN IF NOT EXISTS pdf_path VARCHAR(255);

ALTER TABLE bill_to_pay
    ADD COLUMN IF NOT EXISTS pdf_path VARCHAR(255);

-- ===============================================
-- V20: Criar tabela de documentos fiscais
-- ===============================================
CREATE TABLE IF NOT EXISTS fiscal_document (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    numero_nota VARCHAR(120),
    valor NUMERIC(19, 2),
    data_emissao DATE,
    pdf_path VARCHAR(500) NOT NULL,
    data_upload TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- ===============================================
-- V21: Adicionar placa aos documentos fiscais
-- ===============================================
ALTER TABLE fiscal_document
    ADD COLUMN placa VARCHAR(20);

-- ===============================================
-- V22: Adicionar CNPJ ao parceiro
-- ===============================================
-- Adiciona campo CNPJ na tabela Partner para suportar fornecedores pessoa jurídica
ALTER TABLE partner ADD COLUMN cnpj VARCHAR(14);

-- Cria índice para melhorar performance em buscas por CNPJ
CREATE INDEX idx_partner_cnpj ON partner(cnpj);

-- ===============================================
-- V23: Criar tabela de configuração de NFe
-- ===============================================
-- Cria tabela para armazenar configurações de integração com a SEFAZ
CREATE TABLE nfe_config (
    id BIGSERIAL PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    certificado_path VARCHAR(500) NOT NULL,
    certificado_senha VARCHAR(255) NOT NULL,
    ultimo_nsu VARCHAR(20) NOT NULL DEFAULT '0',
    ambiente VARCHAR(20) NOT NULL DEFAULT 'HOMOLOGACAO',
    uf VARCHAR(2) DEFAULT 'SP',
    ativo BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT chk_ambiente CHECK (ambiente IN ('HOMOLOGACAO', 'PRODUCAO'))
);

-- Comentários nas colunas
COMMENT ON TABLE nfe_config IS 'Configurações para integração com SEFAZ - Consulta de NFe';
COMMENT ON COLUMN nfe_config.cnpj IS 'CNPJ da empresa para consulta na SEFAZ';
COMMENT ON COLUMN nfe_config.certificado_path IS 'Caminho do arquivo .pfx no disco';
COMMENT ON COLUMN nfe_config.certificado_senha IS 'Senha do certificado digital';
COMMENT ON COLUMN nfe_config.ultimo_nsu IS 'Último NSU consultado (para paginação)';
COMMENT ON COLUMN nfe_config.ambiente IS 'Ambiente SEFAZ: HOMOLOGACAO ou PRODUCAO';
COMMENT ON COLUMN nfe_config.uf IS 'UF do emitente';
COMMENT ON COLUMN nfe_config.ativo IS 'Flag para habilitar/desabilitar consulta automática';

-- ===============================================
-- V24: Criar tabela de notas fiscais de entrada
-- ===============================================
-- Cria tabela para armazenar as notas fiscais de entrada (caixa de entrada)
CREATE TABLE incoming_invoice (
    id BIGSERIAL PRIMARY KEY,
    chave_acesso VARCHAR(44) NOT NULL UNIQUE,
    numero_nota VARCHAR(20) NOT NULL,
    cnpj_emitente VARCHAR(14) NOT NULL,
    nome_emitente VARCHAR(255) NOT NULL,
    valor_total DECIMAL(15,2) NOT NULL,
    data_emissao TIMESTAMP NOT NULL,
    xml_content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    bill_to_pay_id BIGINT,
    observacoes TEXT,

    CONSTRAINT chk_status CHECK (status IN ('PENDENTE', 'PROCESSADA', 'IGNORADA'))
);

-- Índices para melhorar performance
CREATE INDEX idx_incoming_invoice_status ON incoming_invoice(status);
CREATE INDEX idx_incoming_invoice_cnpj ON incoming_invoice(cnpj_emitente);
CREATE INDEX idx_incoming_invoice_imported ON incoming_invoice(imported_at);
CREATE INDEX idx_incoming_invoice_chave ON incoming_invoice(chave_acesso);

-- Comentários nas colunas
COMMENT ON TABLE incoming_invoice IS 'Caixa de entrada de notas fiscais importadas da SEFAZ';
COMMENT ON COLUMN incoming_invoice.chave_acesso IS 'Chave de acesso da NFe (44 dígitos)';
COMMENT ON COLUMN incoming_invoice.numero_nota IS 'Número da nota fiscal';
COMMENT ON COLUMN incoming_invoice.cnpj_emitente IS 'CNPJ do emissor (fornecedor)';
COMMENT ON COLUMN incoming_invoice.nome_emitente IS 'Razão social do emissor';
COMMENT ON COLUMN incoming_invoice.valor_total IS 'Valor total da nota';
COMMENT ON COLUMN incoming_invoice.data_emissao IS 'Data e hora de emissão da nota';
COMMENT ON COLUMN incoming_invoice.xml_content IS 'XML completo da nota para processamento';
COMMENT ON COLUMN incoming_invoice.status IS 'Status: PENDENTE, PROCESSADA ou IGNORADA';
COMMENT ON COLUMN incoming_invoice.imported_at IS 'Data/hora de importação';
COMMENT ON COLUMN incoming_invoice.processed_at IS 'Data/hora de processamento';
COMMENT ON COLUMN incoming_invoice.bill_to_pay_id IS 'ID da conta a pagar gerada (se processada)';
COMMENT ON COLUMN incoming_invoice.observacoes IS 'Observações sobre processamento';

-- ===============================================
-- V25: Criar tabela de vendas/CRM
-- ===============================================
-- Tabela para gerenciar vendas/negociacoes do CRM
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    cooperativa VARCHAR(255),
    tipo_veiculo VARCHAR(255),
    placa VARCHAR(50),
    marca VARCHAR(255),
    ano_modelo VARCHAR(20),
    modelo VARCHAR(255),
    nome_contato VARCHAR(255),
    email VARCHAR(255),
    celular VARCHAR(20),
    estado VARCHAR(2),
    cidade VARCHAR(255),
    origem_lead VARCHAR(255),
    veiculo_trabalho BOOLEAN DEFAULT FALSE,
    enviar_cotacao BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'NOVO_LEAD',
    observacoes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sale_status CHECK (status IN ('NOVO_LEAD', 'CONTATO_INICIAL', 'PROPOSTA_ENVIADA', 'NEGOCIACAO', 'FECHADO', 'PERDIDO'))
);

-- Indice para melhorar performance de buscas por status
CREATE INDEX idx_sales_status ON sales(status);

-- Indice para melhorar performance de buscas por nome de contato
CREATE INDEX idx_sales_nome_contato ON sales(nome_contato);

-- Indice para ordenacao por data de criacao
CREATE INDEX idx_sales_created_at ON sales(created_at DESC);

-- ===============================================
-- V26: Atualizar status de vendas para funil de filiação
-- ===============================================
-- Atualiza os status de vendas para refletir o funil de filiacao

-- Remove a constraint antiga
ALTER TABLE sales DROP CONSTRAINT IF EXISTS chk_sale_status;

-- Atualiza os status existentes para os novos valores
UPDATE sales SET status = 'COTACOES_RECEBIDAS' WHERE status = 'NOVO_LEAD';
UPDATE sales SET status = 'EM_NEGOCIACAO' WHERE status IN ('CONTATO_INICIAL', 'PROPOSTA_ENVIADA', 'NEGOCIACAO');
UPDATE sales SET status = 'FILIACAO_CONCRETIZADAS' WHERE status = 'FECHADO';

-- Adiciona a nova constraint com os status corretos
ALTER TABLE sales ADD CONSTRAINT chk_sale_status CHECK (status IN ('COTACOES_RECEBIDAS', 'EM_NEGOCIACAO', 'VISTORIAS', 'LIBERADAS_PARA_CADASTRO', 'FILIACAO_CONCRETIZADAS'));

-- Atualiza o default para novos registros
ALTER TABLE sales ALTER COLUMN status SET DEFAULT 'COTACOES_RECEBIDAS';

-- ===============================================
-- FIM DO SCRIPT
-- ===============================================
-- Script concluído com sucesso!
-- Todas as 26 migrações foram consolidadas.
--
-- PRÓXIMOS PASSOS:
-- 1. Execute este script no banco PostgreSQL
-- 2. Inicie a aplicação Spring Boot
-- 3. O Hibernate criará automaticamente as tabelas JPA restantes
-- 4. Flyway reconhecerá que as migrações já foram aplicadas
--
-- NOTA: Se você executar este script em um banco vazio,
-- algumas migrações podem falhar (como ALTER TABLE em tabelas
-- que ainda não existem). Neste caso:
-- - Ignore os erros de ALTER TABLE em tabelas não existentes
-- - OU comente as migrações que modificam tabelas JPA (V2, V4, V5, V6)
-- - Inicie a aplicação para que o Hibernate crie as tabelas
-- - Execute as migrações comentadas manualmente depois
-- ===============================================
