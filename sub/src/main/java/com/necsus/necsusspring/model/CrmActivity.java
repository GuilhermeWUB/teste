package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_activities")
public class CrmActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status;

    @Enumerated(EnumType.STRING)
    private ActivityPriority prioridade;

    @ManyToOne
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @Column(name = "contato_nome")
    private String contatoNome;

    @Column(name = "contato_email")
    private String contatoEmail;

    @Column(name = "contato_telefone")
    private String contatoTelefone;

    @Column(name = "data_agendada")
    private LocalDateTime dataAgendada;

    @Column(name = "data_realizada")
    private LocalDateTime dataRealizada;

    @Column(name = "responsavel")
    private String responsavel;

    @Column(columnDefinition = "TEXT")
    private String resultado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public ActivityType getTipo() {
        return tipo;
    }

    public void setTipo(ActivityType tipo) {
        this.tipo = tipo;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }

    public ActivityPriority getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(ActivityPriority prioridade) {
        this.prioridade = prioridade;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public String getContatoNome() {
        return contatoNome;
    }

    public void setContatoNome(String contatoNome) {
        this.contatoNome = contatoNome;
    }

    public String getContatoEmail() {
        return contatoEmail;
    }

    public void setContatoEmail(String contatoEmail) {
        this.contatoEmail = contatoEmail;
    }

    public String getContatoTelefone() {
        return contatoTelefone;
    }

    public void setContatoTelefone(String contatoTelefone) {
        this.contatoTelefone = contatoTelefone;
    }

    public LocalDateTime getDataAgendada() {
        return dataAgendada;
    }

    public void setDataAgendada(LocalDateTime dataAgendada) {
        this.dataAgendada = dataAgendada;
    }

    public LocalDateTime getDataRealizada() {
        return dataRealizada;
    }

    public void setDataRealizada(LocalDateTime dataRealizada) {
        this.dataRealizada = dataRealizada;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
