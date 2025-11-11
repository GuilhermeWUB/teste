package com.necsus.necsusspring.event;

import com.necsus.necsusspring.model.NotificationType;
import com.necsus.necsusspring.model.Prioridade;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener para eventos da aplicação que dispara notificações automaticamente.
 * Escuta eventos do sistema e cria notificações apropriadas para os usuários.
 */
@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Exemplo de listener para eventos personalizados.
     * Este é um template que pode ser expandido conforme necessário.
     */

    /**
     * Dispara notificação quando um novo evento é criado
     *
     * Para usar: publique um evento como este no EventService:
     * applicationEventPublisher.publishEvent(new EventCreatedEvent(event, recipient));
     */
    @Async
    @EventListener
    public void handleEventCreated(EventCreatedEvent event) {
        try {
            logger.info("Disparando notificação para novo evento: {}", event.getEventId());

            notificationService.createNotification(
                    event.getRecipient(),
                    "Novo Evento Criado",
                    "Um novo evento foi criado com ID: " + event.getEventId(),
                    NotificationType.EVENT,
                    "/events/" + event.getEventId(),
                    event.getEventId(),
                    "Event",
                    Prioridade.MEDIA
            );

            logger.info("Notificação criada com sucesso para evento ID: {}", event.getEventId());
        } catch (Exception e) {
            logger.error("Erro ao criar notificação para evento ID {}: ", event.getEventId(), e);
        }
    }

    /**
     * Dispara notificação quando um evento é atualizado
     */
    @Async
    @EventListener
    public void handleEventUpdated(EventUpdatedEvent event) {
        try {
            logger.info("Disparando notificação para atualização de evento: {}", event.getEventId());

            notificationService.createNotification(
                    event.getRecipient(),
                    "Evento Atualizado",
                    event.getUpdateMessage(),
                    NotificationType.EVENT,
                    "/events/" + event.getEventId(),
                    event.getEventId(),
                    "Event",
                    Prioridade.MEDIA
            );

            logger.info("Notificação de atualização criada para evento ID: {}", event.getEventId());
        } catch (Exception e) {
            logger.error("Erro ao criar notificação de atualização para evento ID {}: ", event.getEventId(), e);
        }
    }

    /**
     * Dispara notificação quando uma demanda é criada
     */
    @Async
    @EventListener
    public void handleDemandCreated(DemandCreatedEvent event) {
        try {
            logger.info("Disparando notificação para nova demanda: {}", event.getDemandId());

            notificationService.createNotification(
                    event.getRecipient(),
                    "Nova Demanda Atribuída",
                    event.getMessage(),
                    NotificationType.DEMAND,
                    "/demands/" + event.getDemandId(),
                    event.getDemandId(),
                    "Demand",
                    Prioridade.ALTA
            );

            logger.info("Notificação criada para demanda ID: {}", event.getDemandId());
        } catch (Exception e) {
            logger.error("Erro ao criar notificação para demanda ID {}: ", event.getDemandId(), e);
        }
    }

    /**
     * Dispara notificação quando um pagamento é processado
     */
    @Async
    @EventListener
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        try {
            logger.info("Disparando notificação para pagamento: {}", event.getPaymentId());

            notificationService.createNotification(
                    event.getRecipient(),
                    "Notificação de Pagamento",
                    event.getMessage(),
                    NotificationType.PAYMENT,
                    "/pagamentos/" + event.getPaymentId(),
                    event.getPaymentId(),
                    "Payment",
                    Prioridade.ALTA
            );

            logger.info("Notificação criada para pagamento ID: {}", event.getPaymentId());
        } catch (Exception e) {
            logger.error("Erro ao criar notificação para pagamento ID {}: ", event.getPaymentId(), e);
        }
    }

    /**
     * Dispara notificação quando um comunicado é criado
     */
    @Async
    @EventListener
    public void handleComunicadoCreated(ComunicadoCreatedEvent event) {
        try {
            logger.info("Disparando notificação para comunicado: {}", event.getComunicadoId());

            notificationService.createNotification(
                    event.getRecipient(),
                    "Novo Comunicado",
                    event.getMessage(),
                    NotificationType.COMUNICADO,
                    "/comunicados",
                    event.getComunicadoId(),
                    "Comunicado",
                    Prioridade.MEDIA
            );

            logger.info("Notificação criada para comunicado ID: {}", event.getComunicadoId());
        } catch (Exception e) {
            logger.error("Erro ao criar notificação para comunicado ID {}: ", event.getComunicadoId(), e);
        }
    }

    // Classes de eventos customizados

    /**
     * Evento disparado quando um novo evento é criado
     */
    public static class EventCreatedEvent {
        private final Long eventId;
        private final UserAccount recipient;

        public EventCreatedEvent(Long eventId, UserAccount recipient) {
            this.eventId = eventId;
            this.recipient = recipient;
        }

        public Long getEventId() {
            return eventId;
        }

        public UserAccount getRecipient() {
            return recipient;
        }
    }

    /**
     * Evento disparado quando um evento é atualizado
     */
    public static class EventUpdatedEvent {
        private final Long eventId;
        private final UserAccount recipient;
        private final String updateMessage;

        public EventUpdatedEvent(Long eventId, UserAccount recipient, String updateMessage) {
            this.eventId = eventId;
            this.recipient = recipient;
            this.updateMessage = updateMessage;
        }

        public Long getEventId() {
            return eventId;
        }

        public UserAccount getRecipient() {
            return recipient;
        }

        public String getUpdateMessage() {
            return updateMessage;
        }
    }

    /**
     * Evento disparado quando uma demanda é criada
     */
    public static class DemandCreatedEvent {
        private final Long demandId;
        private final UserAccount recipient;
        private final String message;

        public DemandCreatedEvent(Long demandId, UserAccount recipient, String message) {
            this.demandId = demandId;
            this.recipient = recipient;
            this.message = message;
        }

        public Long getDemandId() {
            return demandId;
        }

        public UserAccount getRecipient() {
            return recipient;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Evento disparado quando um pagamento é processado
     */
    public static class PaymentProcessedEvent {
        private final Long paymentId;
        private final UserAccount recipient;
        private final String message;

        public PaymentProcessedEvent(Long paymentId, UserAccount recipient, String message) {
            this.paymentId = paymentId;
            this.recipient = recipient;
            this.message = message;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public UserAccount getRecipient() {
            return recipient;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Evento disparado quando um comunicado é criado
     */
    public static class ComunicadoCreatedEvent {
        private final Long comunicadoId;
        private final UserAccount recipient;
        private final String message;

        public ComunicadoCreatedEvent(Long comunicadoId, UserAccount recipient, String message) {
            this.comunicadoId = comunicadoId;
            this.recipient = recipient;
            this.message = message;
        }

        public Long getComunicadoId() {
            return comunicadoId;
        }

        public UserAccount getRecipient() {
            return recipient;
        }

        public String getMessage() {
            return message;
        }
    }
}
