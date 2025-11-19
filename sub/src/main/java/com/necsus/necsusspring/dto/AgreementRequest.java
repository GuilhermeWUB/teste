package com.necsus.necsusspring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AgreementRequest(
    @NotBlank(message = "Titulo e obrigatorio")
    String titulo,

    @NotBlank(message = "Descricao e obrigatoria")
    String descricao,

    @NotBlank(message = "Parte envolvida e obrigatoria")
    String parteEnvolvida,

    @NotNull(message = "Valor e obrigatorio")
    @Positive(message = "Valor deve ser positivo")
    BigDecimal valor,

    LocalDate dataVencimento,

    LocalDate dataPagamento,

    String observacoes,

    Integer numeroParcelas,

    Integer parcelaAtual,

    String numeroProcesso
) {}
