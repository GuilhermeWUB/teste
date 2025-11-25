# 🔧 Como Resolver o Erro: "relação vistoria já existe"

## 🚨 Problema

Ao iniciar a aplicação, você recebe o erro:
```
ERRO: relação "vistoria" já existe
Migration V14__create_vistoria_table.sql failed
```

## 🔍 Causa Raiz

A tabela `vistoria` já existe no banco de dados, mas o **Flyway** não sabe disso porque a migração V14 (e possivelmente outras) não foi registrada na tabela `flyway_schema_history`.

Isso geralmente acontece quando:
- ✗ Tabelas foram criadas manualmente (fora do Flyway)
- ✗ A tabela `flyway_schema_history` foi apagada ou corrompida
- ✗ Migrações foram executadas diretamente no banco sem o Flyway

## ✅ Solução

### Opção 1: Correção Automática (RECOMENDADA)

Execute o script `fix_flyway_schema_history.sql` no seu banco de dados:

```bash
# Via psql
psql -h localhost -U admin -d ubsystem -f fix_flyway_schema_history.sql

# Via IntelliJ/DataGrip
# 1. Abra o arquivo fix_flyway_schema_history.sql
# 2. Selecione todo o conteúdo
# 3. Execute (Ctrl+Enter ou botão Run)
```

Este script:
1. ✅ Verifica quais migrações já estão registradas
2. ✅ Registra automaticamente as migrações faltantes (V14-V26)
3. ✅ NÃO modifica suas tabelas existentes
4. ✅ Apenas atualiza o controle do Flyway

### Opção 2: Baseline Completo (Mais Rápida)

Se você tem **certeza** de que todas as tabelas existem e estão corretas:

1. Abra o arquivo `fix_flyway_schema_history.sql`
2. Role até o final e **descomente** o bloco `ALTERNATIVA: SCRIPT COMPLETO DE BASELINE`
3. Execute apenas esse bloco comentado

Isso vai:
- Registrar TODAS as migrações (V1 até V26) de uma vez
- Marcar todas como executadas com sucesso

## 🎯 Após Executar a Correção

1. **Verifique** que todas as migrações foram registradas:
   ```sql
   SELECT installed_rank, version, description, success
   FROM flyway_schema_history
   ORDER BY installed_rank;
   ```

   Você deve ver 26 registros (V1 até V26).

2. **Reinicie** sua aplicação Spring Boot

3. **Sucesso!** ✅ A aplicação deve iniciar normalmente.

## 🔄 Prevenção Futura

Para evitar esse problema no futuro:

1. ❌ **NUNCA** crie tabelas manualmente que estão no controle do Flyway
2. ❌ **NUNCA** execute scripts de migração diretamente no banco
3. ✅ **SEMPRE** deixe o Flyway gerenciar as migrações automaticamente
4. ✅ Se precisar fazer alterações:
   - Crie uma nova migração (V27, V28, etc.)
   - Deixe o Spring Boot aplicar automaticamente na inicialização

## 📋 Checklist de Resolução

- [ ] Executei o script `fix_flyway_schema_history.sql`
- [ ] Verifiquei que as 26 migrações estão registradas
- [ ] Reiniciei a aplicação
- [ ] A aplicação iniciou sem erros
- [ ] Testei funcionalidades básicas

## 🆘 Se Ainda Não Funcionar

Se após executar o script você ainda tiver problemas:

### Solução Drástica: Limpar e Recriar

```sql
-- 1. Fazer backup (IMPORTANTE!)
CREATE TABLE flyway_schema_history_backup AS
SELECT * FROM flyway_schema_history;

-- 2. Limpar histórico do Flyway
DELETE FROM flyway_schema_history;

-- 3. Executar o bloco ALTERNATIVA do fix_flyway_schema_history.sql
-- (Aquele que registra todas as 26 migrações de uma vez)
```

### Verificar Configuração do Flyway

Verifique seu `application.properties` ou `application.yml`:

```properties
# Deve estar assim:
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=false
spring.flyway.validate-on-migrate=true

# NÃO deve estar assim:
spring.flyway.clean-disabled=false  # ← Perigo! Limpa o banco!
```

## 📞 Precisa de Ajuda?

Se ainda tiver problemas:
1. Execute a query de diagnóstico do PASSO 1 do script
2. Tire um print da saída
3. Compartilhe comigo

---

**Criado por:** Claude Code
**Data:** 2025-11-25
**Versão:** 1.0
