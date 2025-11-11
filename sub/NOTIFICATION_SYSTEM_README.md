# Sistema de Notificações - Documentação

## Visão Geral

O sistema de notificações foi implementado para permitir que a aplicação envie notificações aos usuários sobre eventos importantes, atualizações e alertas do sistema.

## Arquitetura

O sistema de notificações segue a arquitetura em camadas do projeto:

```
┌─────────────────────────────────────────────┐
│           Controller Layer                   │
│  - NotificationController (REST + Web)      │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│            Service Layer                     │
│  - NotificationService                       │
│  - NotificationEventListener                 │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          Repository Layer                    │
│  - NotificationRepository                    │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│             Entity Layer                     │
│  - Notification                              │
│  - NotificationType (Enum)                   │
│  - NotificationStatus (Enum)                 │
└─────────────────────────────────────────────┘
```

## Componentes

### 1. Entidade Notification

Representa uma notificação no sistema com os seguintes campos:

- `id`: Identificador único
- `recipient`: Usuário destinatário (UserAccount)
- `title`: Título da notificação
- `message`: Mensagem/conteúdo
- `type`: Tipo da notificação (EVENT, DEMAND, PAYMENT, etc)
- `status`: Status (UNREAD, READ, ARCHIVED)
- `createdAt`: Data de criação
- `readAt`: Data de leitura
- `actionUrl`: URL opcional para ação relacionada
- `relatedEntityId`: ID da entidade relacionada (opcional)
- `relatedEntityType`: Tipo da entidade relacionada (opcional)
- `priority`: Prioridade (BAIXA, MEDIA, ALTA, URGENTE)

### 2. NotificationType (Enum)

Tipos de notificações suportados:
- `EVENT`: Relacionado a eventos
- `DEMAND`: Relacionado a demandas/tarefas
- `PAYMENT`: Relacionado a pagamentos
- `BANK_SLIP`: Relacionado a boletos
- `COMUNICADO`: Relacionado a comunicados
- `PARTNER`: Relacionado a parceiros
- `VEHICLE`: Relacionado a veículos
- `SYSTEM`: Notificações do sistema
- `ALERT`: Alertas/urgências
- `INFO`: Informações gerais

### 3. NotificationStatus (Enum)

Status possíveis:
- `UNREAD`: Não lida (estado inicial)
- `READ`: Lida
- `ARCHIVED`: Arquivada

### 4. NotificationService

Serviço principal com métodos para:
- Criar notificações (simples e completas)
- Buscar notificações por usuário, status, tipo
- Marcar como lida/não lida
- Arquivar e deletar notificações
- Buscar notificações de alta prioridade
- Buscar notificações recentes
- Métodos auxiliares específicos (notifyNewEvent, notifyPayment, etc)

### 5. NotificationController

Controller com endpoints REST e páginas web:

#### Endpoints Web:
- `GET /notifications` - Página principal de notificações
- `GET /notifications/{id}` - Visualiza notificação específica

#### API REST:
- `GET /notifications/api/list` - Lista notificações (com filtros)
- `GET /notifications/api/recent/{limit}` - Últimas N notificações
- `GET /notifications/api/unread-count` - Conta não lidas
- `POST /notifications/api/{id}/mark-read` - Marca como lida
- `POST /notifications/api/{id}/mark-unread` - Marca como não lida
- `POST /notifications/api/mark-all-read` - Marca todas como lidas
- `POST /notifications/api/{id}/archive` - Arquiva notificação
- `DELETE /notifications/api/{id}` - Deleta notificação

### 6. NotificationEventListener

Listener de eventos da aplicação que dispara notificações automaticamente.

## Como Usar

### 1. Criar Notificação Simples

```java
@Autowired
private NotificationService notificationService;

// Notificação básica
notificationService.createNotification(
    userAccount,
    "Título da Notificação",
    "Mensagem da notificação",
    NotificationType.EVENT
);
```

### 2. Criar Notificação Completa

```java
notificationService.createNotification(
    userAccount,
    "Novo Evento Criado",
    "O evento #123 foi criado com sucesso",
    NotificationType.EVENT,
    "/events/123",        // URL de ação
    123L,                 // ID da entidade relacionada
    "Event",              // Tipo da entidade
    Prioridade.ALTA       // Prioridade
);
```

### 3. Usar Métodos Auxiliares

```java
// Notificar novo evento
notificationService.notifyNewEvent(userAccount, eventId, "Detalhes do evento");

// Notificar atualização de evento
notificationService.notifyEventUpdate(userAccount, eventId, "Evento atualizado");

// Notificar nova demanda
notificationService.notifyNewDemand(userAccount, demandId, "Título da demanda");

// Notificar pagamento
notificationService.notifyPayment(userAccount, paymentId, "Pagamento recebido");

// Notificar alerta
notificationService.notifyAlert(userAccount, "Alerta Crítico", "Mensagem de alerta");
```

### 4. Integrar com Eventos da Aplicação

Para disparar notificações automaticamente quando algo acontece no sistema, publique eventos:

```java
@Service
public class EventService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public Event createEvent(Event event) {
        // Salva o evento
        Event savedEvent = eventRepository.save(event);

        // Publica evento para disparar notificação
        eventPublisher.publishEvent(
            new NotificationEventListener.EventCreatedEvent(
                savedEvent.getId(),
                recipientUser
            )
        );

        return savedEvent;
    }
}
```

### 5. Buscar Notificações

```java
// Todas as notificações do usuário
List<Notification> all = notificationService.findByRecipient(user);

// Apenas não lidas
List<Notification> unread = notificationService.findUnreadByRecipient(user);

// Apenas não arquivadas
List<Notification> active = notificationService.findNonArchivedByRecipient(user);

// De alta prioridade
List<Notification> priority = notificationService.findHighPriorityUnread(user);

// Últimas 24h
List<Notification> recent = notificationService.findRecentNotifications(user);

// Com paginação
Page<Notification> page = notificationService.findByRecipient(user, pageable);
```

### 6. Gerenciar Notificações

```java
// Marcar como lida
notificationService.markAsRead(notificationId);

// Marcar todas como lidas
int count = notificationService.markAllAsRead(user);

// Arquivar
notificationService.archiveNotification(notificationId);

// Deletar
notificationService.deleteNotification(notificationId);

// Contar não lidas
long unreadCount = notificationService.countUnread(user);
```

## Interface Web

A interface web está disponível em `/notifications` e oferece:

- Visualização de todas as notificações
- Filtros (Todas, Não Lidas, Arquivadas)
- Contador de notificações não lidas
- Badges de tipo e prioridade
- Ações rápidas (marcar como lida, arquivar, deletar)
- Botão para marcar todas como lidas
- Paginação
- Interface responsiva com Bootstrap

## API REST

### Exemplo de uso da API:

```javascript
// Buscar contador de não lidas
fetch('/notifications/api/unread-count')
    .then(response => response.json())
    .then(data => console.log('Não lidas:', data.unreadCount));

// Marcar como lida
fetch('/notifications/api/123/mark-read', {
    method: 'POST',
    headers: {
        'X-CSRF-TOKEN': csrfToken
    }
})
.then(response => response.json())
.then(data => console.log(data.message));

// Marcar todas como lidas
fetch('/notifications/api/mark-all-read', {
    method: 'POST',
    headers: {
        'X-CSRF-TOKEN': csrfToken
    }
})
.then(response => response.json())
.then(data => console.log(`${data.count} notificações marcadas`));
```

## Banco de Dados

### Tabela `notifications`

A migration SQL cria automaticamente:
- Tabela `notifications` com todas as colunas
- Índices otimizados para consultas
- Foreign key para `user_account`
- Constraints para tipos e status
- Comentários nas colunas

### Índices Criados:

- `idx_recipient_status`: Otimiza buscas por usuário e status
- `idx_recipient_created`: Otimiza ordenação por data
- `idx_type_status`: Otimiza filtros por tipo
- `idx_related_entity`: Otimiza buscas por entidade relacionada
- `idx_created_at`: Otimiza buscas temporais

## Tarefas de Manutenção

O sistema inclui métodos para limpeza automática:

```java
// Arquivar notificações lidas antigas (>30 dias)
int archived = notificationService.archiveOldReadNotifications();

// Deletar notificações arquivadas antigas (>90 dias)
int deleted = notificationService.deleteOldArchivedNotifications();
```

**Recomendação**: Configure um job agendado (@Scheduled) para executar essas tarefas periodicamente.

## Testes

Testes unitários completos estão disponíveis em:
- `NotificationServiceTest.java`

Execute os testes com:
```bash
mvn test -Dtest=NotificationServiceTest
```

## Próximos Passos

Sugestões para melhorias futuras:

1. **Notificações em Tempo Real**: Implementar WebSocket para push de notificações
2. **Email**: Enviar notificações por email para notificações importantes
3. **SMS**: Integrar envio de SMS para alertas urgentes
4. **Templates**: Criar sistema de templates para notificações
5. **Preferências**: Permitir usuários configurarem preferências de notificações
6. **Agrupamento**: Agrupar notificações similares
7. **Digest**: Enviar resumos diários/semanais de notificações
8. **Push Notifications**: Implementar notificações push para mobile

## Configuração de Segurança

O sistema respeita as configurações de segurança do Spring Security:
- Endpoints web e API requerem autenticação
- Usuários só podem acessar suas próprias notificações
- CSRF protection está ativo

## Performance

Otimizações implementadas:
- Índices no banco de dados
- Lazy loading de relacionamentos
- Paginação em consultas
- Queries otimizadas com JPQL
- @Async em listeners de eventos

## Suporte

Para dúvidas ou problemas, consulte:
- Código fonte em: `src/main/java/com/necsus/necsusspring/`
- Testes em: `src/test/java/com/necsus/necsusspring/service/`
- Migration SQL em: `src/main/resources/db/migration/`
