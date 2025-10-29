package com.necsus.necsusspring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FipeResponseDTO {

    private String brand;

    @JsonProperty("codeFipe")
    private String codeFipe;

    private String fuel;

    @JsonProperty("fuelAcronym")
    private String fuelAcronym;

    private String model;

    @JsonProperty("modelYear")
    private Integer modelYear;

    private String price;

    @JsonProperty("priceHistory")
    private List<PriceHistory> priceHistory;

    @JsonProperty("referenceMonth")
    private String referenceMonth;

    @JsonProperty("vehicleType")
    private Integer vehicleType;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceHistory {
        private String month;
        private String price;
        private String reference;
    }
}
