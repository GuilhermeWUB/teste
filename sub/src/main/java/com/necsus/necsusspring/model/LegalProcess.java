package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidade que representa um processo jurídico.
 */
@Entity
@Table(name = "legal_processes")
public class LegalProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "autor", nullable = false)
    private String autor;

    @Column(name = "reu", nullable = false)
    private String reu;

    @Column(name = "materia", nullable = false)
    private String materia;

    @Column(name = "numero_processo", nullable = false, unique = true)
    private String numeroProcesso;

    @Column(name = "valor_causa", precision = 19, scale = 2, nullable = false)
    private BigDecimal valorCausa;

    @Column(name = "pedidos", columnDefinition = "TEXT", nullable = false)
    private String pedidos;

    public LegalProcess() {
    }

    public LegalProcess(String autor,
                        String reu,
                        String materia,
                        String numeroProcesso,
                        BigDecimal valorCausa,
                        String pedidos) {
        this.autor = autor;
        this.reu = reu;
        this.materia = materia;
        this.numeroProcesso = numeroProcesso;
        this.valorCausa = valorCausa;
        this.pedidos = pedidos;
    }

    public Long getId() {
        return id;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getReu() {
        return reu;
    }

    public void setReu(String reu) {
        this.reu = reu;
    }

    public String getMateria() {
        return materia;
    }

    public void setMateria(String materia) {
        this.materia = materia;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public BigDecimal getValorCausa() {
        return valorCausa;
    }

    public void setValorCausa(BigDecimal valorCausa) {
        this.valorCausa = valorCausa;
    }

    public String getPedidos() {
        return pedidos;
    }

    public void setPedidos(String pedidos) {
        this.pedidos = pedidos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegalProcess that = (LegalProcess) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
