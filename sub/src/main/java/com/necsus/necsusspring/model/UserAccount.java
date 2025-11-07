package com.necsus.necsusspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = RoleType.USER.getCode();

    @Column(name = "created_at", nullable = false)
    @Getter(AccessLevel.NONE)  // Desabilita o getter gerado pelo Lombok
    @Setter(AccessLevel.NONE)  // Desabilita o setter gerado pelo Lombok
    private LocalDateTime createdAt;

    /**
     * Getter personalizado para createdAt que NUNCA retorna null
     * Isso previne erros de TimeStamp mesmo se o campo estiver null no banco
     */
    public LocalDateTime getCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        return createdAt;
    }

    /**
     * Setter para createdAt que previne valores null
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (role == null) {
            role = RoleType.USER.getCode();
        }
    }
}
