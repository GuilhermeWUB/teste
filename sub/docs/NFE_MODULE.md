# Módulo de Notas Fiscais de Entrada (NFe - SEFAZ)

## 📋 Visão Geral

Este módulo implementa a integração com a SEFAZ para consulta automática de Notas Fiscais Eletrônicas (NFe) emitidas contra o CNPJ da empresa. As notas são importadas automaticamente e podem ser transformadas em contas a pagar.

## 🏗️ Arquitetura

### Componentes Principais

1. **NfeConfig** - Configuração de certificado digital e credenciais SEFAZ
2. **IncomingInvoice** - Caixa de entrada de notas importadas
3. **NfeIntegrationService** - Robô que consulta a SEFAZ
4. **InvoiceProcessorService** - Transforma notas em contas a pagar
5. **NfeScheduledTask** - Job agendado (roda a cada hora)

### Fluxo de Dados

```
SEFAZ → NfeIntegrationService → IncomingInvoice (PENDENTE)
                                        ↓
                             InvoiceProcessorService
                                        ↓
                              BillToPay + Partner (Fornecedor)
```

## 🚀 Como Usar

### 1. Configuração Inicial

**Endpoint:** `POST /api/nfe/config`

**Form Data:**
- `cnpj` - CNPJ da empresa
- `senha` - Senha do certificado digital
- `uf` - Sigla do estado (ex: SP)
- `ambiente` - HOMOLOGACAO ou PRODUCAO
- `certificado` - Arquivo .pfx do certificado digital

**Exemplo com cURL:**
```bash
curl -X POST http://localhost:8080/api/nfe/config \
  -F "cnpj=12345678000190" \
  -F "senha=minhaSenha123" \
  -F "uf=SP" \
  -F "ambiente=HOMOLOGACAO" \
  -F "certificado=@certificado.pfx"
```

### 2. Sincronização Manual

**Endpoint:** `POST /api/nfe/sync`

Força uma sincronização imediata com a SEFAZ (sem esperar o job agendado).

```bash
curl -X POST http://localhost:8080/api/nfe/sync
```

**Resposta:**
```json
{
  "success": true,
  "message": "Sincronização concluída",
  "notasImportadas": 5
}
```

### 3. Listar Notas Pendentes

**Endpoint:** `GET /api/nfe/inbox/pendentes`

Retorna todas as notas com status PENDENTE.

```bash
curl http://localhost:8080/api/nfe/inbox/pendentes
```

### 4. Processar uma Nota

**Endpoint:** `POST /api/nfe/{id}/process`

Transforma uma nota da caixa de entrada em conta a pagar.

```bash
curl -X POST http://localhost:8080/api/nfe/123/process
```

**O que acontece:**
- Verifica se o fornecedor (CNPJ) já existe em Partner
- Se não existir, cria um novo Partner automaticamente
- Cria um BillToPay com:
  - Descrição: "NFe {numero} - {fornecedor}"
  - Valor: valor total da nota
  - Vencimento: data emissão + 30 dias
  - Status: Pendente

### 5. Ignorar uma Nota

**Endpoint:** `POST /api/nfe/{id}/ignore?motivo=Duplicada`

Marca a nota como IGNORADA (não será processada).

```bash
curl -X POST "http://localhost:8080/api/nfe/123/ignore?motivo=Nota%20duplicada"
```

### 6. Processar Todas em Lote

**Endpoint:** `POST /api/nfe/process-all`

Processa todas as notas pendentes de uma vez.

```bash
curl -X POST http://localhost:8080/api/nfe/process-all
```

### 7. Estatísticas

**Endpoint:** `GET /api/nfe/stats`

Retorna contadores de notas por status.

```bash
curl http://localhost:8080/api/nfe/stats
```

**Resposta:**
```json
{
  "total": 150,
  "pendentes": 23,
  "processadas": 120,
  "ignoradas": 7
}
```

## 📊 Tabelas do Banco

### nfe_config
- `id` - ID da configuração
- `cnpj` - CNPJ da empresa
- `certificado_path` - Caminho do arquivo .pfx
- `certificado_senha` - Senha do certificado
- `ultimo_nsu` - Último NSU consultado (paginação)
- `ambiente` - HOMOLOGACAO ou PRODUCAO
- `uf` - UF do emitente
- `ativo` - Habilita/desabilita consulta automática

### incoming_invoice
- `id` - ID da nota
- `chave_acesso` - Chave de 44 dígitos (única)
- `numero_nota` - Número da NFe
- `cnpj_emitente` - CNPJ do fornecedor
- `nome_emitente` - Razão social
- `valor_total` - Valor da nota
- `data_emissao` - Data de emissão
- `xml_content` - XML completo
- `status` - PENDENTE | PROCESSADA | IGNORADA
- `imported_at` - Data de importação
- `processed_at` - Data de processamento
- `bill_to_pay_id` - ID da conta a pagar gerada
- `observacoes` - Observações

## ⏰ Job Agendado

O job `NfeScheduledTask` roda **a cada hora** automaticamente.

**Cron Expression:** `0 0 * * * *` (minuto 0 de cada hora)

Para alterar a frequência, edite:
```java
@Scheduled(cron = "0 0 * * * *") // A cada hora
// ou
@Scheduled(cron = "0 0 */6 * * *") // A cada 6 horas
```

## 🔒 Segurança

- Todos os endpoints `/api/nfe/**` requerem autenticação
- Apenas usuários com roles ADMIN podem acessar
- Certificado digital armazenado em `certificados/nfe/`

## 🛠️ Tecnologias

- **Java_NFe** (Samuel Oliveira) - Biblioteca de integração SEFAZ
- **Spring Boot 3.1.5**
- **JAXB** - Parse de XML
- **PostgreSQL** - Banco de dados
- **Flyway** - Migrations

## 📝 Logs

O sistema loga todas as operações importantes:

```
[INFO] Iniciando consulta de notas na SEFAZ...
[INFO] Consultando a partir do NSU: 0
[INFO] Status SEFAZ: 138 - Documento localizado
[INFO] Total de notas importadas nesta leva: 5
[INFO] Nota importada com sucesso: 12345 - Fornecedor XYZ - R$ 1500.00
```

## 🐛 Tratamento de Erros

### Certificado Inválido
```json
{
  "success": false,
  "message": "Certificado inválido ou não encontrado. Verifique a configuração."
}
```

### Nota Já Processada
```json
{
  "success": false,
  "message": "Esta nota já foi processada"
}
```

### Erro SEFAZ
- O sistema loga o erro mas não para a aplicação
- Tenta parse manual do XML se JAXB falhar
- Usa recursão para paginação (maxNSU vs ultNSU)

## 🔄 Paginação SEFAZ

A SEFAZ retorna documentos em lotes. O sistema:

1. Consulta a partir do `ultimoNsu` armazenado
2. Processa todos os documentos retornados
3. Atualiza `ultimoNsu` para `ultNSU` retornado
4. Se `ultNSU < maxNSU`, chama recursivamente para buscar mais
5. Continua até não haver mais documentos

## 📞 Suporte

Para dúvidas sobre a biblioteca Java_NFe:
- GitHub: https://github.com/Samuel-Oliveira/Java_NFe
- Wiki: https://github.com/Samuel-Oliveira/Java_NFe/wiki

## 📄 Licença

Este módulo utiliza a biblioteca open-source Java_NFe (MIT License).
