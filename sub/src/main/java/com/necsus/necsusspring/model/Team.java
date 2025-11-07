package com.necsus.necsusspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa um Time/Equipe no sistema
 * Cada time geralmente corresponde a um cargo/role específico
 */
@Entity
@Table(name = "teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Código do role associado a este time (ex: "RH", "FINANCEIRO", etc.)
     * Pode ser null se o time não estiver diretamente associado a um role
     */
    @Column(name = "role_code", length = 50)
    private String roleCode;

    /**
     * Indica se o time está ativo
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Retorna o nome de exibição do role associado
     */
    public String getRoleDisplayName() {
        if (roleCode != null) {
            return RoleType.displayNameFor(roleCode);
        }
        return null;
    }
}
