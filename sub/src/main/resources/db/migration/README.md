# Database Migrations

Este diretório contém scripts SQL para ajustes manuais no banco de dados.

## Como executar os scripts

### Opção 1: Via psql (linha de comando)

```bash
psql -h localhost -U admin -d ubsystem -f fix_vehicle_nullable.sql
```

### Opção 2: Via pgAdmin ou outro cliente PostgreSQL

1. Conecte-se ao banco de dados `ubsystem`
2. Abra o arquivo `fix_vehicle_nullable.sql`
3. Execute o script SQL

### Opção 3: Via Docker (se o PostgreSQL está em container)

```bash
docker exec -i <container_id> psql -U admin -d ubsystem < fix_vehicle_nullable.sql
```

## Scripts disponíveis

### fix_vehicle_nullable.sql
**Problema:** Erro "null value in column 'vehicle_id' violates not-null constraint"

**Solução:** Remove a constraint NOT NULL da coluna `vehicle_id` na tabela `event`, permitindo que eventos sejam criados sem um veículo associado.

**Quando executar:** Execute este script uma vez antes de reiniciar a aplicação após o deploy das alterações que tornam o veículo opcional.

### fix_user_created_at.sql
**Problema:** Erro de TimeStamp ao acessar a aba de usuários

**Solução:** Atualiza todos os registros da tabela `app_users` que têm a coluna `created_at` como NULL, definindo a data atual como valor padrão.

**Quando executar:** Execute este script imediatamente se você está enfrentando erros ao acessar a página de usuários do sistema.

## Verificação

Após executar o script, você pode verificar se funcionou:

```sql
SELECT column_name, is_nullable
FROM information_schema.columns
WHERE table_name = 'event' AND column_name = 'vehicle_id';
```

O resultado deve mostrar `is_nullable = YES`.
