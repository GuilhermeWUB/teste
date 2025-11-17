package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de Demanda - Sistema de hierarquia
 * Permite que diretores passem demandas para cargos específicos
 */
@Entity
@Table(name = "demands")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DemandStatus status = DemandStatus.PENDENTE;

    @Enumerated(EnumType.STRING)
    private DemandPriority prioridade = DemandPriority.MEDIA;

    /**
     * Usuário que criou a demanda (geralmente DIRETORIA)
     */
    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private UserAccount createdBy;

    /**
     * Roles (cargos) que devem receber esta demanda
     * Armazenado como lista de strings separadas por vírgula
     * Ex: "RH,FINANCEIRO,TI"
     */
    @Column(name = "target_roles")
    private String targetRoles;

    /**
     * Usuário específico ao qual a demanda foi atribuída (opcional)
     */
    @ManyToOne
    @JoinColumn(name = "assigned_to_id")
    private UserAccount assignedTo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completion_observation", columnDefinition = "TEXT")
    private String completionObservation;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = DemandStatus.PENDENTE;
        }
        if (prioridade == null) {
            prioridade = DemandPriority.MEDIA;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == DemandStatus.CONCLUIDA && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    /**
     * Retorna lista de roles destinatários
     */
    public List<String> getTargetRolesList() {
        if (targetRoles == null || targetRoles.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(targetRoles.split(","));
    }

    /**
     * Define lista de roles destinatários
     */
    public void setTargetRolesList(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            this.targetRoles = null;
        } else {
            this.targetRoles = String.join(",", roles);
        }
    }

    public String getPrioridadeColor() {
        if (prioridade == null) {
            return "#64748b"; // slate
        }
        return switch (prioridade) {
            case URGENTE -> "#dc2626";
            case ALTA -> "#f97316";
            case MEDIA -> "#0ea5e9";
            case BAIXA -> "#10b981";
        };
    }
}
