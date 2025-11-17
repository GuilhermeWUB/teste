package com.necsus.necsusspring.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vistoria_foto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VistoriaFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vistoria_id", nullable = false)
    @JsonBackReference
    private Vistoria vistoria;

    @Column(name = "foto_path", nullable = false, length = 500)
    private String fotoPath;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
