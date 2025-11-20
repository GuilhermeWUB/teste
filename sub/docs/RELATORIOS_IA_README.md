# 🤖 Relatórios Dinâmicos com IA - Documentação

## Visão Geral

A funcionalidade de **Relatórios Dinâmicos com IA** permite que usuários façam perguntas em linguagem natural e obtenham dados do banco de dados instantaneamente. O sistema usa a API do Google Gemini para converter perguntas em consultas SQL e executá-las de forma segura.

## Como Funciona

1. **Usuário faz uma pergunta** em linguagem natural
   - Exemplo: "Quantos veículos ativos temos?"

2. **IA converte em SQL**
   - O Gemini 2.5 Flash gera uma query SQL baseada no esquema do banco
   - Exemplo gerado: `SELECT COUNT(*) as total FROM vehicle WHERE vehicle_status = 'ACTIVE'`

3. **Sistema valida e executa**
   - O SQL é sanitizado e validado (apenas SELECT permitido)
   - Query é executada no banco de dados

4. **Resultados são exibidos**
   - Dados são formatados e exibidos em uma tabela interativa

## Acesso

### Interface Web
- Navegue para: **Relatórios > Relatórios com IA**
- Ou acesse diretamente: `https://seu-dominio.com/reports/ai`

### Exemplos de Perguntas

| Categoria | Pergunta de Exemplo |
|-----------|---------------------|
| **Veículos** | Quantos veículos ativos temos? |
| **Veículos** | Mostre os veículos por montadora |
| **Parceiros** | Liste os 10 parceiros mais recentes |
| **Parceiros** | Quantos parceiros temos por cidade? |
| **Financeiro** | Qual o total de mensalidades? |
| **Financeiro** | Mostre as mensalidades acima de R$ 500 |
| **Eventos** | Quantos eventos pendentes existem? |
| **Jurídico** | Quantos processos jurídicos estão em aberto? |

## Segurança

### Camadas de Proteção

#### 1. Sanitização de SQL
- ✅ Apenas comandos `SELECT` são permitidos
- ❌ Bloqueados: `DELETE`, `UPDATE`, `DROP`, `ALTER`, `CREATE`, `INSERT`, `TRUNCATE`, etc.
- ❌ Bloqueados: Múltiplos comandos (`;` múltiplos)
- ❌ Bloqueados: Comentários SQL (`--`, `/*`, `*/`)

#### 2. Usuário de Banco de Dados com Permissões Limitadas

**⚠️ IMPORTANTE: Para máxima segurança em produção, configure um usuário de banco de dados com permissões SOMENTE de leitura.**

##### Criar Usuário de Leitura

Execute o script SQL localizado em `docs/SECURITY_DB_READONLY_USER.sql`:

```sql
-- Criar usuário
CREATE USER sub_leitor WITH PASSWORD 'sua_senha_forte_aqui';

-- Conceder permissões de leitura
GRANT CONNECT ON DATABASE seu_banco TO sub_leitor;
GRANT USAGE ON SCHEMA public TO sub_leitor;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO sub_leitor;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO sub_leitor;
```

##### Configurar no Spring Boot (Produção)

**Opção 1: DataSource Separado (RECOMENDADO)**

Crie um segundo DataSource apenas para relatórios. Isso requer modificações no código:

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource mainDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.readonly")
    public DataSource readOnlyDataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

```properties
# application-prod.properties

# DataSource principal (com permissões completas)
spring.datasource.url=jdbc:postgresql://localhost:5432/seu_banco
spring.datasource.username=usuario_principal
spring.datasource.password=senha_principal

# DataSource somente leitura (para relatórios IA)
spring.datasource.readonly.url=jdbc:postgresql://localhost:5432/seu_banco
spring.datasource.readonly.username=sub_leitor
spring.datasource.readonly.password=senha_forte_aqui
```

**Opção 2: Usar como Usuário Principal (NÃO RECOMENDADO)**

Isso limitará TODAS as operações do sistema a SELECT apenas:

```properties
# application.properties
spring.datasource.username=sub_leitor
spring.datasource.password=senha_forte_aqui
```

**⚠️ AVISO: Esta opção impedirá que o sistema crie, atualize ou delete dados!**

## Arquitetura Técnica

### Backend

#### 1. GeminiService.java
```java
public List<Map<String, Object>> gerarRelatorioPorTexto(String pergunta)
```
- Recebe pergunta em linguagem natural
- Gera SQL usando Gemini API
- Sanitiza e valida SQL
- Executa query no banco
- Retorna resultados

#### 2. GenerativeReportController.java
```java
POST /api/relatorios-ia/gerar
Body: { "pergunta": "Quantos veículos ativos temos?" }
```
- Endpoint REST para geração de relatórios
- Validação de entrada
- Tratamento de erros
- Retorna JSON com dados

#### 3. ReportController.java
```java
GET /reports/ai
```
- Renderiza a página de relatórios com IA

### Frontend

#### 1. relatorio_ia.html
- Interface de usuário com campo de input
- Exemplos de perguntas clicáveis
- Renderização dinâmica de tabelas
- Formatação automática de valores

#### 2. Funcionalidades JavaScript
- `gerarRelatorioIA()` - Faz requisição à API
- `renderizarTabela()` - Cria tabela HTML dinamicamente
- `formatarNomeColuna()` - Formata nomes de colunas
- `formatarValor()` - Formata valores (moeda, data, etc.)

## Esquema do Banco de Dados

O sistema conhece as seguintes tabelas:

| Tabela | Descrição |
|--------|-----------|
| `app_users` | Usuários do sistema |
| `partner` | Parceiros/Associados |
| `vehicle` | Veículos cadastrados |
| `event` | Eventos registrados |
| `info_payment` | Informações de pagamento |
| `legal_processes` | Processos jurídicos |

Para ver o esquema completo com todas as colunas, consulte o método `buildSqlGenerationPrompt()` em `GeminiService.java`.

## Limitações

1. **Apenas consultas SELECT**
   - Não é possível modificar dados via IA

2. **Dependência da API Gemini**
   - Requer chave de API válida
   - Sujeito a limites de taxa da API

3. **Precisão do SQL**
   - A IA pode gerar SQL incorreto em alguns casos
   - Sempre revise os resultados

4. **Performance**
   - Queries complexas podem demorar
   - Sem cache de resultados atualmente

## Troubleshooting

### Erro: "SQL deve começar com SELECT"
**Causa:** A IA gerou um comando que não é SELECT
**Solução:** Reformule a pergunta para ser mais específica sobre consulta de dados

### Erro: "Nenhum SQL gerado pela API"
**Causa:** Problema na comunicação com Gemini API
**Solução:** Verifique a chave de API e conexão com internet

### Erro de Conexão
**Causa:** Backend não está respondendo
**Solução:** Verifique se o serviço está rodando e se o endpoint `/api/relatorios-ia/gerar` está acessível

### Resultados Vazios
**Causa:** Query SQL gerada não retornou dados
**Solução:** Reformule a pergunta ou verifique se os dados existem no banco

## Manutenção e Monitoramento

### Logs
O sistema registra:
- Perguntas recebidas
- SQL gerado
- Erros de validação
- Resultados das queries

```java
logger.info("Gerando relatório para pergunta: {}", pergunta);
logger.info("Executando SQL gerado: {}", sql);
logger.info("Relatório gerado com sucesso. {} linhas retornadas", resultados.size());
```

### Monitoramento de Segurança

Monitore queries suspeitas no banco de dados:

```sql
-- Ver queries executadas pelo usuário de leitura
SELECT * FROM pg_stat_activity WHERE usename = 'sub_leitor';

-- Ver histórico de queries (se log_statement = 'all')
SELECT * FROM pg_stat_statements WHERE usename = 'sub_leitor';
```

## Configuração da API Gemini

### Obter Chave de API
1. Acesse: https://makersuite.google.com/app/apikey
2. Crie uma nova chave de API
3. Copie a chave

### Configurar no Spring Boot

```properties
# application.properties
gemini.api.key=SUA_CHAVE_AQUI
gemini.api.model=gemini-2.0-flash-exp
```

### Limites da API
- **Gratuito**: 15 requisições por minuto
- **Pago**: Varia conforme plano

## Desenvolvimento Futuro

### Melhorias Planejadas
- [ ] Cache de queries frequentes
- [ ] Histórico de perguntas do usuário
- [ ] Exportação de resultados (Excel, CSV)
- [ ] Gráficos e visualizações automáticas
- [ ] Sugestões de perguntas baseadas em uso
- [ ] DataSource separado para leitura
- [ ] Rate limiting por usuário
- [ ] Auditoria de queries executadas

## Suporte

Para problemas ou dúvidas:
- Verifique os logs da aplicação
- Consulte a documentação do código
- Entre em contato com a equipe de desenvolvimento

---

**Última atualização:** 2025-01-20
**Versão:** 1.0.0
