# 🚀 Setup do CRM - Backend e Frontend Completo

## ⚠️ IMPORTANTE: Resolver Erro de Schema Validation

Antes de rodar a aplicação, você precisa executar as migrations manualmente no PostgreSQL.

### Erro que você está vendo:

```
Schema-validation: missing column [concluida] in table [sales]
```

### ✅ Solução:

Execute o script SQL que está na raiz do projeto:

```bash
psql -h localhost -U postgres -d postgres -f execute_migrations.sql
```

Ou copie e cole o conteúdo do arquivo `execute_migrations.sql` diretamente no pgAdmin ou qualquer cliente PostgreSQL.

---

## 📋 O que foi implementado

### 🔧 Backend (100% Funcional)

#### **Vendas**
- ✅ CRUD completo de vendas
- ✅ Campo `valorVenda` para armazenar valor da venda
- ✅ Campo `concluida` (boolean)
- ✅ Campo `dataConclusao` (timestamp)
- ✅ Endpoint `POST /crm/api/vendas/{id}/concluir` para concluir venda
- ✅ Endpoint `GET /crm/api/vendas/concluidas`
- ✅ Métricas: total, concluídas, receita total e mensal, taxa de conversão

#### **Atividades**
- ✅ CRUD completo de atividades CRM
- ✅ Entidade `CrmActivity` com todos os campos
- ✅ Tipos: Ligação, Email, Reunião, Visita, Follow-up, Apresentação, Negociação, Vistoria, Outro
- ✅ Status: Agendada, Em andamento, Concluída, Cancelada, Reagendada
- ✅ Prioridades: Baixa, Média, Alta, Urgente
- ✅ Relacionamento com vendas (Sale)
- ✅ Dados de contato completos
- ✅ Endpoints REST completos:
  - GET /crm/api/atividades
  - GET /crm/api/atividades/{id}
  - GET /crm/api/atividades/status/{status}
  - GET /crm/api/atividades/tipo/{tipo}
  - GET /crm/api/atividades/venda/{saleId}
  - GET /crm/api/atividades/responsavel/{nome}
  - GET /crm/api/atividades/recentes
  - POST /crm/api/atividades
  - PUT /crm/api/atividades/{id}
  - PUT /crm/api/atividades/{id}/status
  - DELETE /crm/api/atividades/{id}

#### **Dashboard**
- ✅ Endpoint `/crm/api/dashboard/metrics` - métricas completas
- ✅ Endpoint `/crm/api/dashboard/vendas` - apenas vendas
- ✅ Endpoint `/crm/api/dashboard/atividades` - apenas atividades
- ✅ Cálculo de receita total e mensal
- ✅ Taxa de conversão
- ✅ Contadores por status e tipo

### 🎨 Frontend (UI/UX Moderna)

#### **Dashboard (/crm/dashboard)**
- ✅ Cards animados com métricas em tempo real
- ✅ Total de vendas
- ✅ Vendas concluídas com progress bar
- ✅ Receita total e do mês (formatado em R$)
- ✅ Taxa de conversão com barra visual
- ✅ Funil de vendas (vendas por status)
- ✅ Métricas de atividades (total, agendadas, concluídas, hoje)
- ✅ Distribuição de atividades por tipo com ícones
- ✅ Botões de ação rápida
- ✅ Loading states
- ✅ Design responsivo
- ✅ Dark mode support
- ✅ Animations suaves

#### **Atividades (/crm/atividades)**
- ✅ Lista de atividades em cards visuais
- ✅ Filtros em tempo real:
  - Por status
  - Por tipo
  - Busca por texto
- ✅ Mini dashboard com stats
- ✅ Modal para criar/editar atividades
- ✅ Formulário completo com validação
- ✅ Ações nos cards:
  - Editar
  - Excluir
  - Marcar como concluída
- ✅ Badges coloridos por status
- ✅ Formatação de datas
- ✅ Empty states
- ✅ Loading states
- ✅ Design responsivo mobile-first

---

## 🗄️ Estrutura do Banco de Dados

### Tabela `sales` (atualizada)
```sql
- id (BIGSERIAL)
- cooperativa, tipo_veiculo, placa, marca, ano_modelo, modelo
- nome_contato, email, celular, estado, cidade, origem_lead
- veiculo_trabalho (BOOLEAN)
- enviar_cotacao (BOOLEAN)
- status (VARCHAR)
- observacoes (TEXT)
- valor_venda (DOUBLE PRECISION) ⭐ NOVO
- data_conclusao (TIMESTAMP) ⭐ NOVO
- concluida (BOOLEAN) ⭐ NOVO
- created_at, updated_at
```

### Tabela `crm_activities` (nova)
```sql
- id (BIGSERIAL)
- titulo (VARCHAR 255)
- descricao (TEXT)
- tipo (VARCHAR 50)
- status (VARCHAR 50)
- prioridade (VARCHAR 50)
- sale_id (BIGINT FK)
- contato_nome, contato_email, contato_telefone
- data_agendada, data_realizada
- responsavel
- resultado (TEXT)
- created_at, updated_at
```

---

## 🔥 Como Usar

### 1. Executar Migrations

```bash
# Conecte no PostgreSQL e execute:
psql -h localhost -U postgres -d postgres -f execute_migrations.sql
```

### 2. Iniciar Aplicação

```bash
cd sub
mvn spring-boot:run
```

### 3. Acessar CRM

```
http://localhost:8080/crm/dashboard
http://localhost:8080/crm/atividades
http://localhost:8080/crm/vendas
```

### 4. Testar APIs

#### Criar Atividade
```bash
curl -X POST http://localhost:8080/crm/api/atividades \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Ligação de follow-up",
    "tipo": "LIGACAO",
    "prioridade": "ALTA",
    "contatoNome": "João Silva",
    "contatoTelefone": "(11) 99999-9999",
    "dataAgendada": "2025-11-27T10:00:00",
    "responsavel": "Maria Santos"
  }'
```

#### Concluir Venda
```bash
curl -X POST http://localhost:8080/crm/api/vendas/1/concluir \
  -H "Content-Type: application/json" \
  -d '{"valorVenda": 25000.00}'
```

#### Ver Métricas Dashboard
```bash
curl http://localhost:8080/crm/api/dashboard/metrics
```

---

## 🎯 Funcionalidades

### Dashboard
- ✅ Visualização de todas as métricas em tempo real
- ✅ Botão "Atualizar" para recarregar dados
- ✅ Formatação de valores em Real (R$)
- ✅ Animações suaves ao carregar
- ✅ Responsivo para mobile

### Atividades
- ✅ Criar nova atividade com formulário completo
- ✅ Editar atividade existente
- ✅ Excluir atividade
- ✅ Marcar como concluída com um clique
- ✅ Filtrar por status, tipo e buscar por texto
- ✅ Visualizar detalhes completos no card

### Vendas
- ✅ Concluir venda com valor
- ✅ Status muda automaticamente para "Filiação concretizadas"
- ✅ Data de conclusão registrada automaticamente

---

## 📊 Tecnologias Utilizadas

### Backend
- Spring Boot 3.1.5
- Spring Data JPA
- PostgreSQL
- Flyway Migrations

### Frontend
- HTML5 + CSS3
- JavaScript Vanilla
- Bootstrap Icons
- Fetch API
- CSS Grid & Flexbox

---

## 🐛 Troubleshooting

### Erro: "Missing column"
Execute o script `execute_migrations.sql`

### Erro: "Connection refused"
Verifique se o PostgreSQL está rodando na porta 5432

### Erro: "Authentication failed"
Verifique as credenciais no `application.properties`

### Frontend não carrega dados
Abra o DevTools (F12) e veja o console para erros de CORS ou autenticação

---

## 📝 Próximos Passos

Para melhorar ainda mais o CRM, considere:

1. **Notificações**: Implementar sistema de notificações para atividades próximas
2. **Calendário**: Adicionar visualização de calendário para atividades
3. **Relatórios**: Criar página de relatórios com gráficos
4. **Export**: Permitir exportar dados para Excel/PDF
5. **Anexos**: Adicionar suporte para anexar arquivos às atividades
6. **Automação**: Criar workflows automáticos
7. **Integração**: Integrar com e-mail e WhatsApp

---

## ✅ Checklist de Deployment

- [ ] Executar `execute_migrations.sql` no banco de produção
- [ ] Configurar variáveis de ambiente (DB_HOST, POSTGRES_USER, etc.)
- [ ] Revisar permissões de acesso (roles do Spring Security)
- [ ] Fazer backup do banco de dados
- [ ] Testar todas as funcionalidades em staging
- [ ] Monitorar logs após deploy

---

## 📞 Suporte

Em caso de dúvidas ou problemas:
1. Verifique os logs da aplicação em `app.log`
2. Consulte a documentação do Spring Boot
3. Revise o código-fonte nos controllers e services

---

**Desenvolvido com ❤️ usando Spring Boot + JavaScript**
