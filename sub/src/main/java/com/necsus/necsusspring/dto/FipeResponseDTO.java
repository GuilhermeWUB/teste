package com.necsus.necsusspring.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FipeResponseDTO {

    @JsonAlias("marca")
    private String brand;

    @JsonAlias("codigoFipe")
    private String codeFipe;

    @JsonAlias("combustivel")
    private String fuel;

    @JsonAlias("siglaCombustivel")
    private String fuelAcronym;

    @JsonAlias({"modelo", "nome"})
    private String model;

    @JsonAlias("anoModelo")
    private Integer modelYear;

    @JsonAlias({"valor", "precoMedio"})
    private String price;

    @JsonAlias({"historico", "priceHistory"})
    private List<PriceHistory> priceHistory;

    @JsonAlias({"mesReferencia", "referencia"})
    private String referenceMonth;

    @JsonAlias({"tipoVeiculo", "TipoVeiculo"})
    private Integer vehicleType;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceHistory {
        @JsonAlias({"mes", "month"})
        private String month;
        @JsonAlias("preco")
        private String price;
        @JsonAlias("referencia")
        private String reference;
    }
}
