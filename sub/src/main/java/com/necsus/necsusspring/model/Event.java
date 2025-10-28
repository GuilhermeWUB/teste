package com.necsus.necsusspring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Informe o motivo")
    @Enumerated(EnumType.STRING)
    private Motivo motivo;

    @NotNull(message = "Informe o envolvimento")
    @Enumerated(EnumType.STRING)
    private Envolvimento envolvimento;

    @NotNull(message = "Informe a data do ocorrido")
    private Date dataAconteceu;

    @NotNull(message = "Informe o horário do ocorrido")
    private Long horaAconteceu;

    @NotNull(message = "Informe a data da comunicação")
    private Date dataComunicacao;

    @NotNull(message = "Informe o horário da comunicação")
    private Long horaComunicacao;

    private Long idExterno;

    private String analistaResponsavel;
}
