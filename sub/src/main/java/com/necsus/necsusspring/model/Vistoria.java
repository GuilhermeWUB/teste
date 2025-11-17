package com.necsus.necsusspring.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "vistoria", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    @JsonManagedReference
    private List<VistoriaFoto> fotos = new ArrayList<>();

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
        return fotos != null ? fotos.size() : 0;
    }

    // Método auxiliar para adicionar foto
    public void adicionarFoto(VistoriaFoto foto) {
        if (fotos == null) {
            fotos = new ArrayList<>();
        }
        fotos.add(foto);
        foto.setVistoria(this);
    }

    // Método auxiliar para remover foto
    public void removerFoto(VistoriaFoto foto) {
        if (fotos != null) {
            fotos.remove(foto);
            foto.setVistoria(null);
        }
    }
}
