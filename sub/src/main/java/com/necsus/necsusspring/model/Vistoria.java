package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vistoria")
public class Vistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Informe o evento/comunicado")
    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "foto1_path")
    private String foto1Path;

    @Column(name = "foto2_path")
    private String foto2Path;

    @Column(name = "foto3_path")
    private String foto3Path;

    @Column(name = "foto4_path")
    private String foto4Path;

    @Column(name = "foto5_path")
    private String foto5Path;

    @Column(name = "foto6_path")
    private String foto6Path;

    @Column(name = "foto7_path")
    private String foto7Path;

    @Column(name = "foto8_path")
    private String foto8Path;

    @Column(name = "foto9_path")
    private String foto9Path;

    @Column(name = "foto10_path")
    private String foto10Path;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "usuario_criacao")
    private String usuarioCriacao;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }

    // Método auxiliar para contar quantas fotos foram anexadas
    public int getQuantidadeFotos() {
        int count = 0;
        if (foto1Path != null && !foto1Path.isEmpty()) count++;
        if (foto2Path != null && !foto2Path.isEmpty()) count++;
        if (foto3Path != null && !foto3Path.isEmpty()) count++;
        if (foto4Path != null && !foto4Path.isEmpty()) count++;
        if (foto5Path != null && !foto5Path.isEmpty()) count++;
        if (foto6Path != null && !foto6Path.isEmpty()) count++;
        if (foto7Path != null && !foto7Path.isEmpty()) count++;
        if (foto8Path != null && !foto8Path.isEmpty()) count++;
        if (foto9Path != null && !foto9Path.isEmpty()) count++;
        if (foto10Path != null && !foto10Path.isEmpty()) count++;
        return count;
    }
}
