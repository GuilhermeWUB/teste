package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "regionais")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Regional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime getCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public Regional(Long id, String name, String code, String description, Boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.active = (active != null) ? active : true;
        this.setCreatedAt(createdAt);
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }

    @PostLoad
    public void postLoad() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
