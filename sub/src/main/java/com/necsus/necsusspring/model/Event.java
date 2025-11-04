
package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o título")
    @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
    private String titulo;

    @NotBlank(message = "Informe a descrição")
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @NotNull(message = "Informe o status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Prioridade prioridade;

    @NotNull(message = "Informe o motivo")
    @Enumerated(EnumType.STRING)
    private Motivo motivo;

    @NotNull(message = "Informe o envolvimento")
    @Enumerated(EnumType.STRING)
    private Envolvimento envolvimento;


    private LocalDate dataAconteceu;


    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaAconteceu;


    private LocalDate dataComunicacao;


    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaComunicacao;


    private LocalDate dataVencimento;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    private Long idExterno;

    private String analistaResponsavel;

    @NotNull(message = "Informe o associado")
    @ManyToOne
    @JoinColumn(name = "partner_id")
    private Partner partner;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Size(max = 10, message = "A placa deve ter no máximo 10 caracteres")
    private String placaManual;

    // Método auxiliar para exibir prioridade formatada
    public String getPrioridadeFormatted() {
        return prioridade != null ? prioridade.getDisplayName() : "";
    }

    // Método auxiliar para exibir status formatado
    public String getStatusFormatted() {
        return status != null ? status.getDisplayName() : "";
    }

    // Método auxiliar para obter cor da prioridade (para o frontend)
    public String getPrioridadeColor() {
        if (prioridade == null) return "secondary";
        return switch (prioridade) {
            case BAIXA -> "success";
            case MEDIA -> "info";
            case ALTA -> "warning";
            case URGENTE -> "danger";
        };
    }
}
