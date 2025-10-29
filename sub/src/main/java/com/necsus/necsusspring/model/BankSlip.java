package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

@Entity
@Table(name = "bank_slips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankSlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "partners_id")
    private Partner partner;

    @Column(name = "vencimento")
    @Temporal(TemporalType.DATE)
    private Date vencimento;

    @Column(name = "valor")
    private BigDecimal valor;

    @Column(name = "status")
    private Integer status;

    @Column(name = "nosso_numero")
    private String nossoNumero;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Column(name = "data_pagamento")
    @Temporal(TemporalType.DATE)
    private Date dataPagamento;

    @Column(name = "valor_recebido")
    private BigDecimal valorRecebido;

    @Column(name = "data_documento")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataDocumento;

    @Column(name = "data_processamento")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataProcessamento;

    @Column(name = "quantidade")
    private Integer quantidade;

    @Column(name = "slips_briefing_id")
    private Long slipsBriefingId;

    @Column(name = "status_remessa")
    private Integer statusRemessa;

    @ManyToOne
    @JoinColumn(name = "adhesion_id")
    private Adhesion adhesion;

    @ManyToOne
    @JoinTable(
        name = "bank_shipment_has_bank_slips",
        joinColumns = @JoinColumn(name = "bank_slips_id"),
        inverseJoinColumns = @JoinColumn(name = "bank_shipment_id")
    )
    private BankShipment bankShipment;

    @ManyToOne
    @JoinColumn(name = "slips_briefing_id", insertable = false, updatable = false)
    private SlipsBriefing slipsBriefing;
}
