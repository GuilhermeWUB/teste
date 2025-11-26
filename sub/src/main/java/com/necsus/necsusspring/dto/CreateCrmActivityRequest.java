package com.necsus.necsusspring.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateCrmActivityRequest(
        @NotBlank(message = "O título é obrigatório")
        String title,

        String description,

        @NotBlank(message = "O status é obrigatório")
        @Pattern(regexp = "atrasada|para-hoje|planejada|concluida", message = "Status inválido")
        String status,

        @NotBlank(message = "O tipo é obrigatório")
        String type,

        @NotBlank(message = "O responsável é obrigatório")
        String responsible,

        @NotBlank(message = "A origem do lead é obrigatória")
        String leadSource,

        String city,
        String state,

        @NotNull(message = "A data de vencimento é obrigatória")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dueDate,

        @NotNull(message = "O horário é obrigatório")
        @JsonFormat(pattern = "HH:mm")
        LocalTime dueTime
) {
}
