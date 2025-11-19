package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Notification;
import com.necsus.necsusspring.model.NotificationStatus;
import com.necsus.necsusspring.model.NotificationType;
import com.necsus.necsusspring.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para operações de banco de dados relacionadas a notificações.
 * Fornece métodos de consulta customizados para gerenciar notificações de usuários.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Busca todas as notificações de um usuário ordenadas por data de criação (mais recentes primeiro)
     */
    List<Notification> findByRecipientOrderByCreatedAtDesc(UserAccount recipient);

    /**
     * Busca notificações de um usuário com paginação
     */
    Page<Notification> findByRecipientOrderByCreatedAtDesc(UserAccount recipient, Pageable pageable);

    /**
     * Busca notificações não lidas de um usuário
     */
    List<Notification> findByRecipientAndStatusOrderByCreatedAtDesc(UserAccount recipient, NotificationStatus status);

    /**
     * Busca notificações não lidas de um usuário com paginação
     */
    Page<Notification> findByRecipientAndStatusOrderByCreatedAtDesc(UserAccount recipient, NotificationStatus status, Pageable pageable);

    /**
     * Busca notificações de um usuário por tipo
     */
    List<Notification> findByRecipientAndTypeOrderByCreatedAtDesc(UserAccount recipient, NotificationType type);

    /**
     * Busca notificações de um usuário por tipo e status
     */
    List<Notification> findByRecipientAndTypeAndStatusOrderByCreatedAtDesc(
            UserAccount recipient,
            NotificationType type,
            NotificationStatus status
    );

    /**
     * Conta o número de notificações não lidas de um usuário
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient = :recipient AND n.status = com.necsus.necsusspring.model.NotificationStatus.UNREAD")
    long countUnreadByRecipient(@Param("recipient") UserAccount recipient);

    /**
     * Conta o número de notificações por status para um usuário
     */
    long countByRecipientAndStatus(UserAccount recipient, NotificationStatus status);

    /**
     * Conta todas as notificações de um usuário
     */
    long countByRecipient(UserAccount recipient);

    /**
     * Busca notificações recentes de um usuário (últimas 24 horas)
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("recipient") UserAccount recipient, @Param("since") LocalDateTime since);

    /**
     * Busca notificações não arquivadas de um usuário
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient AND n.status <> com.necsus.necsusspring.model.NotificationStatus.ARCHIVED ORDER BY n.createdAt DESC")
    List<Notification> findNonArchivedByRecipient(@Param("recipient") UserAccount recipient);

    /**
     * Busca notificações não arquivadas de um usuário com paginação
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient AND n.status <> com.necsus.necsusspring.model.NotificationStatus.ARCHIVED ORDER BY n.createdAt DESC")
    Page<Notification> findNonArchivedByRecipient(@Param("recipient") UserAccount recipient, Pageable pageable);

    /**
     * Busca notificações relacionadas a uma entidade específica
     */
    List<Notification> findByRelatedEntityTypeAndRelatedEntityId(String entityType, Long entityId);

    /**
     * Busca notificações de alta prioridade não lidas de um usuário
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient AND n.status = com.necsus.necsusspring.model.NotificationStatus.UNREAD " +
           "AND (n.priority = com.necsus.necsusspring.model.Prioridade.ALTA OR n.priority = com.necsus.necsusspring.model.Prioridade.URGENTE) ORDER BY n.createdAt DESC")
    List<Notification> findHighPriorityUnreadByRecipient(@Param("recipient") UserAccount recipient);

    /**
     * Marca todas as notificações não lidas de um usuário como lidas
     */
    @Modifying
    @Query("UPDATE Notification n SET n.status = com.necsus.necsusspring.model.NotificationStatus.READ, n.readAt = :readAt WHERE n.recipient = :recipient AND n.status = com.necsus.necsusspring.model.NotificationStatus.UNREAD")
    int markAllAsReadForRecipient(@Param("recipient") UserAccount recipient, @Param("readAt") LocalDateTime readAt);

    /**
     * Arquiva notificações lidas antigas (mais de X dias)
     */
    @Modifying
    @Query("UPDATE Notification n SET n.status = com.necsus.necsusspring.model.NotificationStatus.ARCHIVED WHERE n.status = com.necsus.necsusspring.model.NotificationStatus.READ AND n.readAt < :cutoffDate")
    int archiveOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Deleta notificações arquivadas antigas (mais de X dias)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.status = com.necsus.necsusspring.model.NotificationStatus.ARCHIVED AND n.createdAt < :cutoffDate")
    int deleteOldArchivedNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Busca as últimas N notificações de um usuário
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient ORDER BY n.createdAt DESC")
    List<Notification> findTopNByRecipient(@Param("recipient") UserAccount recipient, Pageable pageable);
}
