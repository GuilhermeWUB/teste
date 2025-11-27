package com.necsus.necsusspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
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

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    @Getter(AccessLevel.NONE)  // Desabilita o getter gerado pelo Lombok
    @Setter(AccessLevel.NONE)  // Desabilita o setter gerado pelo Lombok
    private LocalDateTime createdAt = LocalDateTime.now();  // Inicialização direta

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
     * Setter personalizado para createdAt que previne valores null
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    /**
     * Construtor completo que garante que createdAt nunca será null
     * Substitui o @AllArgsConstructor do Lombok para ter controle total
     */
    public UserAccount(Long id, String fullName, String username, String email,
                       String password, String role, Boolean active, BigDecimal saldo, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = (active != null) ? active : true;
        this.saldo = (saldo != null) ? saldo : BigDecimal.ZERO;
        // Usa o setter personalizado para garantir que não seja null
        this.setCreatedAt(createdAt);
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (role == null) {
            role = RoleType.USER.getCode();
        }
        if (active == null) {
            active = true;
        }
        if (saldo == null) {
            saldo = BigDecimal.ZERO;
        }
    }

    /**
     * Corrige valores null IMEDIATAMENTE após carregar do banco
     * Isso é a última linha de defesa contra erros de TimeStamp
     */
    @PostLoad
    public void postLoad() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
