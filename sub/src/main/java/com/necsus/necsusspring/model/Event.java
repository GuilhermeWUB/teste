package com.necsus.necsusspring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Informe o motivo")
    @Enumerated(EnumType.STRING)
    private EventReason motivo;

    @NotNull(message = "Informe o envolvimento")
    @Enumerated(EnumType.STRING)
    private EventInvolvement envolvimento;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventReason getMotivo() {
        return motivo;
    }

    public void setMotivo(EventReason motivo) {
        this.motivo = motivo;
    }

    public EventInvolvement getEnvolvimento() {
        return envolvimento;
    }

    public void setEnvolvimento(EventInvolvement envolvimento) {
        this.envolvimento = envolvimento;
    }

    public Date getDataAconteceu() {
        return dataAconteceu;
    }

    public void setDataAconteceu(Date dataAconteceu) {
        this.dataAconteceu = dataAconteceu;
    }

    public Long getHoraAconteceu() {
        return horaAconteceu;
    }

    public void setHoraAconteceu(Long horaAconteceu) {
        this.horaAconteceu = horaAconteceu;
    }

    public Date getDataComunicacao() {
        return dataComunicacao;
    }

    public void setDataComunicacao(Date dataComunicacao) {
        this.dataComunicacao = dataComunicacao;
    }

    public Long getHoraComunicacao() {
        return horaComunicacao;
    }

    public void setHoraComunicacao(Long horaComunicacao) {
        this.horaComunicacao = horaComunicacao;
    }

    public Long getIdExterno() {
        return idExterno;
    }

    public void setIdExterno(Long idExterno) {
        this.idExterno = idExterno;
    }

    public String getAnalistaResponsavel() {
        return analistaResponsavel;
    }

    public void setAnalistaResponsavel(String analistaResponsavel) {
        this.analistaResponsavel = analistaResponsavel;
    }
}
