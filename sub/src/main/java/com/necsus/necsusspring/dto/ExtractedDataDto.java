package com.necsus.necsusspring.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedDataDto {
    private String numeroNota;
    private String dataEmissao;
    private String valor;
    private String placa;
}
