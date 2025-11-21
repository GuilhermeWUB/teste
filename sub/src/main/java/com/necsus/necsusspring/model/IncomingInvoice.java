package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade para armazenar as notas fiscais de entrada importadas da SEFAZ
 * Funciona como uma "caixa de entrada" antes de serem processadas e transformadas em contas a pagar
 */
@Entity
@Table(name = "incoming_invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomingInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Chave de acesso da NFe (44 dígitos) - única
     */
    @Column(nullable = false, unique = true, length = 44)
    private String chaveAcesso;

    /**
     * Número da nota fiscal
     */
    @Column(nullable = false)
    private String numeroNota;

    /**
     * CNPJ do emissor da nota (fornecedor)
     */
    @Column(nullable = false, length = 14)
    private String cnpjEmitente;

    /**
     * Razão social do emissor
     */
    @Column(nullable = false)
    private String nomeEmitente;

    /**
     * Valor total da nota fiscal
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    /**
     * Data e hora de emissão da nota
     */
    @Column(nullable = false)
    private LocalDateTime dataEmissao;

    /**
     * XML completo da nota (para processamento posterior)
     */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String xmlContent;

    /**
     * Status da nota na caixa de entrada
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncomingInvoiceStatus status = IncomingInvoiceStatus.PENDENTE;

    /**
     * Data e hora de importação da nota
     */
    @Column(nullable = false)
    private LocalDateTime importedAt = LocalDateTime.now();

    /**
     * Data e hora de processamento (quando foi transformada em conta a pagar)
     */
    private LocalDateTime processedAt;

    /**
     * ID da conta a pagar criada a partir desta nota (se já foi processada)
     */
    @Column(name = "bill_to_pay_id")
    private Long billToPayId;

    /**
     * Observações sobre o processamento
     */
    @Column(columnDefinition = "TEXT")
    private String observacoes;
}
