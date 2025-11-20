package com.necsus.necsusspring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para dados extraídos de nota fiscal pelo Gemini
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedDataDto {
    private String numeroNota;
    private String dataEmissao;  // formato YYYY-MM-DD
    private String valor;         // com ponto como separador decimal
    private String placa;
}
