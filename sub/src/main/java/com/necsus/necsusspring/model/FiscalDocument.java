package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fiscal_document")
public class FiscalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "numero_nota")
    private String numeroNota;

    @Column(precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(length = 20)
    private String placa;

    @Column(name = "data_emissao")
    @Temporal(TemporalType.DATE)
    private Date dataEmissao;

    @Column(name = "pdf_path", nullable = false)
    private String pdfPath;

    @Column(name = "data_upload")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataUpload;

    @PrePersist
    protected void onCreate() {
        if (dataUpload == null) {
            dataUpload = new Date();
        }
    }
}
