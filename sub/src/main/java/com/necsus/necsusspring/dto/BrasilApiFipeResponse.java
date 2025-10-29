package com.necsus.necsusspring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrasilApiFipeResponse {

    private String valor;
    private String marca;
    private String modelo;
    private Integer anoModelo;
    private String combustivel;
    private String siglaCombustivel;
    private String codigoFipe;
    private String mesReferencia;
    private Integer tipoVeiculo;
}
