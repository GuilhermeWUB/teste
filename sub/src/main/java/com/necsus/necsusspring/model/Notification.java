package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma notificação no sistema.
 * Notificações são mensagens direcionadas a usuários específicos,
 * informando sobre eventos, atualizações ou ações necessárias.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_recipient_status", columnList = "recipient_id,status"),
        @Index(name = "idx_recipient_created", columnList = "recipient_id,created_at"),
        @Index(name = "idx_type_status", columnList = "type,status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuário que receberá a notificação
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    @NotNull(message = "O destinatário da notificação é obrigatório")
    private UserAccount recipient;

    /**
     * Título da notificação
     */
    @NotBlank(message = "O título da notificação é obrigatório")
    @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
    @Column(nullable = false)
    private String title;

    /**
     * Mensagem/conteúdo da notificação
     */
    @NotBlank(message = "A mensagem da notificação é obrigatória")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * Tipo da notificação (EVENT, DEMAND, PAYMENT, etc)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "O tipo da notificação é obrigatório")
    private NotificationType type;

    /**
     * Status da notificação (UNREAD, READ, ARCHIVED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "O status da notificação é obrigatório")
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    /**
     * Data e hora de criação da notificação
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Data e hora em que a notificação foi lida
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * URL/Link relacionado à notificação (opcional)
     * Pode ser usado para redirecionar o usuário ao contexto da notificação
     */
    @Size(max = 500, message = "A URL deve ter no máximo 500 caracteres")
    private String actionUrl;

    /**
     * ID da entidade relacionada (Event, Demand, Payment, etc) - opcional
     * Permite vincular a notificação a uma entidade específica
     */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /**
     * Tipo da entidade relacionada (Event, Demand, Payment, etc) - opcional
     * Usado em conjunto com relatedEntityId
     */
    @Size(max = 100, message = "O tipo da entidade relacionada deve ter no máximo 100 caracteres")
    @Column(name = "related_entity_type", length = 100)
    private String relatedEntityType;

    /**
     * Prioridade da notificação (seguindo o mesmo padrão de Prioridade do sistema)
     * HIGH = alta prioridade, MEDIUM = média, LOW = baixa
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Prioridade priority;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = NotificationStatus.UNREAD;
        }
    }

    /**
     * Marca a notificação como lida
     */
    public void markAsRead() {
        if (this.status == NotificationStatus.UNREAD) {
            this.status = NotificationStatus.READ;
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * Marca a notificação como não lida
     */
    public void markAsUnread() {
        this.status = NotificationStatus.UNREAD;
        this.readAt = null;
    }

    /**
     * Arquiva a notificação
     */
    public void archive() {
        this.status = NotificationStatus.ARCHIVED;
    }

    /**
     * Verifica se a notificação está não lida
     */
    public boolean isUnread() {
        return this.status == NotificationStatus.UNREAD;
    }

    /**
     * Verifica se a notificação está lida
     */
    public boolean isRead() {
        return this.status == NotificationStatus.READ;
    }

    /**
     * Verifica se a notificação está arquivada
     */
    public boolean isArchived() {
        return this.status == NotificationStatus.ARCHIVED;
    }

    /**
     * Verifica se a notificação tem alta prioridade
     */
    public boolean isHighPriority() {
        return this.priority == Prioridade.ALTA || this.priority == Prioridade.URGENTE;
    }
}
