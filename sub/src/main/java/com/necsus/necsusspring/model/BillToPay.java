package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Modelo para representar boletos/contas a pagar (saídas financeiras)
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bill_to_pay")
public class BillToPay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "data_vencimento", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dataVencimento;

    @Column(name = "data_pagamento")
    @Temporal(TemporalType.DATE)
    private Date dataPagamento;

    @Column(name = "valor_pago")
    private BigDecimal valorPago;

    // 0 = Pendente, 1 = Pago
    @Column(nullable = false)
    private Integer status = 0;

    private String fornecedor;

    private String categoria;

    private String observacao;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "data_criacao")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCriacao;

    @PrePersist
    protected void onCreate() {
        if (dataCriacao == null) {
            dataCriacao = new Date();
        }
        if (status == null) {
            status = 0;
        }
    }
}
