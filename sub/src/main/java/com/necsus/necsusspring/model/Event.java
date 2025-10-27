package com.necsus.necsusspring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o motivo")
    private String motivo;

    @NotBlank(message = "Informe o envolvimento")
    private String envolvimento;

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

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getEnvolvimento() {
        return envolvimento;
    }

    public void setEnvolvimento(String envolvimento) {
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
