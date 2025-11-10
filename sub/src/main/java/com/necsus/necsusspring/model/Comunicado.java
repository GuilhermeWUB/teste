
package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comunicado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o título")
    @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
    private String titulo;

    @NotBlank(message = "Informe a mensagem")
    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataExpiracao;

    @Size(max = 100, message = "O nome do autor deve ter no máximo 100 caracteres")
    private String autor;

    @Column(nullable = false)
    private boolean ativo = true;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }

    // Método auxiliar para verificar se o comunicado está expirado
    public boolean isExpirado() {
        if (dataExpiracao == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(dataExpiracao);
    }

    // Método auxiliar para verificar se o comunicado está visível
    public boolean isVisivel() {
        return ativo && !isExpirado();
    }
}
