# Database Migrations

Este diretório contém as migrations gerenciadas pelo **Flyway**.

## ⚠️ IMPORTANTE: Migrations Automáticas com Flyway

A partir desta versão, o projeto usa **Flyway** para gerenciar o schema do banco de dados automaticamente.

### O que mudou:
- ✅ **Migrations automáticas**: Flyway aplica as migrations automaticamente na inicialização
- ✅ **Versionamento**: Todas as migrations seguem o padrão `V{número}__{descrição}.sql`
- ✅ **Controle de estado**: Flyway rastreia quais migrations já foram aplicadas
- ⚠️ **Hibernate em modo validate**: O Hibernate apenas valida o schema, não o modifica mais

### Como funciona:
1. Ao iniciar a aplicação, o Flyway verifica a tabela `flyway_schema_history`
2. Identifica quais migrations ainda não foram aplicadas
3. Executa automaticamente as migrations pendentes em ordem
4. Registra o sucesso ou falha de cada migration

### ⚠️ NÃO execute migrations manualmente!

As migrations serão aplicadas automaticamente pelo Flyway. Executar manualmente pode causar conflitos.

## Estrutura das Migrations

Todas as migrations seguem o padrão Flyway:
- `V1__create_legal_processes_table.sql` - Cria tabela de processos jurídicos
- `V2__add_document_fields_to_event.sql` - Adiciona campos de documento em eventos
- `V3__create_notifications_table.sql` - Cria tabela de notificações
- `V4__fix_user_created_at.sql` - Corrige campo created_at em users
- `V5__fix_vehicle_nullable.sql` - Torna vehicle_id opcional
- `V6__migrate_event_status_to_new_flow.sql` - Migra status de eventos
- `V7__add_status_to_legal_processes.sql` - Adiciona coluna status
- `V8__add_process_type_to_legal_processes.sql` - Adiciona tipo de cobrança
- `V9__update_legal_process_status_check.sql` - **Atualiza constraint com regex**
- `V11__expand_legal_process_status_values.sql` - Garante suporte a todos os status mapeados na aplicação

## Migration V9: Correção da Constraint

A migration V9 resolve o erro de constraint ao criar processos RASTREADOR/FIDELIDADE:

```sql
-- Permite qualquer status utilizado pela aplicação ao cadastrar processos
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
```

### Como executar manualmente (apenas em desenvolvimento/debug)

Se você precisar executar uma migration específica manualmente:

```bash
# Via psql
psql -h localhost -U admin -d ubsystem -f V9__update_legal_process_status_check.sql

# Via Docker
docker exec -i <container_id> psql -U admin -d ubsystem < V9__update_legal_process_status_check.sql
```

## Scripts disponíveis

### fix_vehicle_nullable.sql
**Problema:** Erro "null value in column 'vehicle_id' violates not-null constraint"

**Solução:** Remove a constraint NOT NULL da coluna `vehicle_id` na tabela `event`, permitindo que eventos sejam criados sem um veículo associado.

**Quando executar:** Execute este script uma vez antes de reiniciar a aplicação após o deploy das alterações que tornam o veículo opcional.

### fix_user_created_at.sql
**Problema:** Erro de TimeStamp ao acessar a aba de usuários

**Solução:** Correção completa em 3 passos:
1. Atualiza todos os registros com `created_at` NULL para a data atual
2. Define um valor DEFAULT para novos registros
3. Adiciona constraint NOT NULL para prevenir futuros problemas

**Quando executar:** Execute este script IMEDIATAMENTE se você está enfrentando erros ao acessar a página de usuários do sistema.

**IMPORTANTE:** A partir da versão corrigida do código, a aplicação também possui proteções em nível de código para prevenir erros de TimeStamp, mas é ESSENCIAL executar este script no banco de dados primeiro.

## Verificação

Após executar o script, você pode verificar se funcionou:

```sql
SELECT column_name, is_nullable
FROM information_schema.columns
WHERE table_name = 'event' AND column_name = 'vehicle_id';
```

O resultado deve mostrar `is_nullable = YES`.
