package com.necsus.necsusspring.dto;

import com.necsus.necsusspring.model.LegalProcessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * DTO para criação/atualização de processos jurídicos.
 */
public record LegalProcessRequest(
        @NotBlank(message = "Autor é obrigatório")
        String autor,

        @NotBlank(message = "Réu é obrigatório")
        String reu,

        @NotBlank(message = "Matéria é obrigatória")
        String materia,

        @NotBlank(message = "Número do processo é obrigatório")
        String numeroProcesso,

        @NotNull(message = "Valor da causa é obrigatório")
        @PositiveOrZero(message = "Valor da causa deve ser zero ou positivo")
        BigDecimal valorCausa,

        @NotBlank(message = "Pedidos são obrigatórios")
        String pedidos,

        @NotNull(message = "Tipo de cobrança é obrigatório")
        LegalProcessType processType,

        Long sourceEventId,

        String sourceEventSnapshot
) {
}
