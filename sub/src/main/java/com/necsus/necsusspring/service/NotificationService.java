package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciar notificações do sistema.
 * Fornece métodos para criar, ler, atualizar e deletar notificações,
 * além de funcionalidades específicas como marcar como lida/não lida e arquivar.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Cria uma nova notificação
     */
    @Transactional
    public Notification createNotification(UserAccount recipient, String title, String message,
                                          NotificationType type) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .status(NotificationStatus.UNREAD)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Cria uma nova notificação com todos os parâmetros
     */
    @Transactional
    public Notification createNotification(UserAccount recipient, String title, String message,
                                          NotificationType type, String actionUrl,
                                          Long relatedEntityId, String relatedEntityType,
                                          Prioridade priority) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .status(NotificationStatus.UNREAD)
                .actionUrl(actionUrl)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .priority(priority)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Busca uma notificação por ID
     */
    @Transactional(readOnly = true)
    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    /**
     * Busca todas as notificações de um usuário
     */
    @Transactional(readOnly = true)
    public List<Notification> findByRecipient(UserAccount recipient) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    /**
     * Busca notificações de um usuário com paginação
     */
    @Transactional(readOnly = true)
    public Page<Notification> findByRecipient(UserAccount recipient, Pageable pageable) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient, pageable);
    }

    /**
     * Busca notificações não lidas de um usuário
     */
    @Transactional(readOnly = true)
    public List<Notification> findUnreadByRecipient(UserAccount recipient) {
        return notificationRepository.findByRecipientAndStatusOrderByCreatedAtDesc(
                recipient, NotificationStatus.UNREAD);
    }

    /**
     * Busca notificações não lidas de um usuário com paginação
     */
    @Transactional(readOnly = true)
    public Page<Notification> findUnreadByRecipient(UserAccount recipient, Pageable pageable) {
        return notificationRepository.findByRecipientAndStatusOrderByCreatedAtDesc(
                recipient, NotificationStatus.UNREAD, pageable);
    }

    /**
     * Busca notificações não arquivadas de um usuário
     */
    @Transactional(readOnly = true)
    public List<Notification> findNonArchivedByRecipient(UserAccount recipient) {
        return notificationRepository.findNonArchivedByRecipient(recipient);
    }

    /**
     * Busca notificações não arquivadas de um usuário com paginação
     */
    @Transactional(readOnly = true)
    public Page<Notification> findNonArchivedByRecipient(UserAccount recipient, Pageable pageable) {
        return notificationRepository.findNonArchivedByRecipient(recipient, pageable);
    }

    /**
     * Busca notificações por tipo
     */
    @Transactional(readOnly = true)
    public List<Notification> findByRecipientAndType(UserAccount recipient, NotificationType type) {
        return notificationRepository.findByRecipientAndTypeOrderByCreatedAtDesc(recipient, type);
    }

    /**
     * Busca notificações de alta prioridade não lidas
     */
    @Transactional(readOnly = true)
    public List<Notification> findHighPriorityUnread(UserAccount recipient) {
        return notificationRepository.findHighPriorityUnreadByRecipient(recipient);
    }

    /**
     * Busca notificações recentes (últimas 24 horas)
     */
    @Transactional(readOnly = true)
    public List<Notification> findRecentNotifications(UserAccount recipient) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return notificationRepository.findRecentNotifications(recipient, since);
    }

    /**
     * Busca as últimas N notificações de um usuário
     */
    @Transactional(readOnly = true)
    public List<Notification> findTopN(UserAccount recipient, int limit) {
        return notificationRepository.findTopNByRecipient(recipient, PageRequest.of(0, limit));
    }

    /**
     * Conta notificações não lidas de um usuário
     */
    @Transactional(readOnly = true)
    public long countUnread(UserAccount recipient) {
        return notificationRepository.countUnreadByRecipient(recipient);
    }

    /**
     * Conta notificações por status
     */
    @Transactional(readOnly = true)
    public long countByStatus(UserAccount recipient, NotificationStatus status) {
        return notificationRepository.countByRecipientAndStatus(recipient, status);
    }

    /**
     * Marca uma notificação como lida
     */
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada com id " + notificationId));

        notification.markAsRead();
        return notificationRepository.save(notification);
    }

    /**
     * Marca uma notificação como não lida
     */
    @Transactional
    public Notification markAsUnread(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada com id " + notificationId));

        notification.markAsUnread();
        return notificationRepository.save(notification);
    }

    /**
     * Marca todas as notificações de um usuário como lidas
     */
    @Transactional
    public int markAllAsRead(UserAccount recipient) {
        return notificationRepository.markAllAsReadForRecipient(recipient, LocalDateTime.now());
    }

    /**
     * Arquiva uma notificação
     */
    @Transactional
    public Notification archiveNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada com id " + notificationId));

        notification.archive();
        return notificationRepository.save(notification);
    }

    /**
     * Deleta uma notificação
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        if (notificationRepository.existsById(notificationId)) {
            notificationRepository.deleteById(notificationId);
        } else {
            throw new RuntimeException("Notificação não encontrada com id " + notificationId);
        }
    }

    /**
     * Arquiva notificações lidas antigas (mais de 30 dias)
     */
    @Transactional
    public int archiveOldReadNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        return notificationRepository.archiveOldReadNotifications(cutoffDate);
    }

    /**
     * Deleta notificações arquivadas antigas (mais de 90 dias)
     */
    @Transactional
    public int deleteOldArchivedNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        return notificationRepository.deleteOldArchivedNotifications(cutoffDate);
    }

    /**
     * Busca notificações relacionadas a uma entidade
     */
    @Transactional(readOnly = true)
    public List<Notification> findByRelatedEntity(String entityType, Long entityId) {
        return notificationRepository.findByRelatedEntityTypeAndRelatedEntityId(entityType, entityId);
    }

    // ========== Métodos auxiliares para criar notificações específicas ==========

    /**
     * Cria notificação para novo evento
     */
    @Transactional
    public Notification notifyNewEvent(UserAccount recipient, Long eventId, String eventDetails) {
        return createNotification(
                recipient,
                "Novo Evento Criado",
                "Um novo evento foi criado: " + eventDetails,
                NotificationType.EVENT,
                "/events/" + eventId,
                eventId,
                "Event",
                Prioridade.MEDIA
        );
    }

    /**
     * Cria notificação para atualização de evento
     */
    @Transactional
    public Notification notifyEventUpdate(UserAccount recipient, Long eventId, String updateDetails) {
        return createNotification(
                recipient,
                "Evento Atualizado",
                "O evento foi atualizado: " + updateDetails,
                NotificationType.EVENT,
                "/events/" + eventId,
                eventId,
                "Event",
                Prioridade.MEDIA
        );
    }

    /**
     * Cria notificação para nova demanda
     */
    @Transactional
    public Notification notifyNewDemand(UserAccount recipient, Long demandId, String demandTitle) {
        return createNotification(
                recipient,
                "Nova Demanda Atribuída",
                "Você foi atribuído à demanda: " + demandTitle,
                NotificationType.DEMAND,
                "/demands/" + demandId,
                demandId,
                "Demand",
                Prioridade.ALTA
        );
    }

    /**
     * Cria notificação para pagamento
     */
    @Transactional
    public Notification notifyPayment(UserAccount recipient, Long paymentId, String paymentDetails) {
        return createNotification(
                recipient,
                "Notificação de Pagamento",
                paymentDetails,
                NotificationType.PAYMENT,
                "/pagamentos/" + paymentId,
                paymentId,
                "Payment",
                Prioridade.ALTA
        );
    }

    /**
     * Cria notificação para boleto
     */
    @Transactional
    public Notification notifyBankSlip(UserAccount recipient, Long bankSlipId, String bankSlipDetails) {
        return createNotification(
                recipient,
                "Notificação de Boleto",
                bankSlipDetails,
                NotificationType.BANK_SLIP,
                "/boletos/" + bankSlipId,
                bankSlipId,
                "BankSlip",
                Prioridade.ALTA
        );
    }

    /**
     * Cria notificação de sistema
     */
    @Transactional
    public Notification notifySystem(UserAccount recipient, String title, String message) {
        return createNotification(recipient, title, message, NotificationType.SYSTEM);
    }

    /**
     * Cria notificação de alerta
     */
    @Transactional
    public Notification notifyAlert(UserAccount recipient, String title, String message) {
        return createNotification(
                recipient,
                title,
                message,
                NotificationType.ALERT,
                null,
                null,
                null,
                Prioridade.URGENTE
        );
    }
}
