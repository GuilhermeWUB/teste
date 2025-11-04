package com.necsus.necsusspring.dto;

import com.necsus.necsusspring.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transferir dados básicos de veículos para o frontend.
 * Usado especialmente no dropdown de seleção de veículos no formulário de eventos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {

    private Long id;
    private String plaque;
    private String maker;
    private String model;
    private String color;
    private String yearMod;

    /**
     * Construtor de conveniência que cria um DTO a partir de uma entidade Vehicle.
     * @param vehicle A entidade Vehicle
     */
    public VehicleDTO(Vehicle vehicle) {
        if (vehicle != null) {
            this.id = vehicle.getId();
            this.plaque = vehicle.getPlaque();
            this.maker = vehicle.getMaker();
            this.model = vehicle.getModel();
            this.color = vehicle.getColor();
            this.yearMod = vehicle.getYear_mod();
        }
    }

    /**
     * Converte uma entidade Vehicle para DTO.
     * @param vehicle A entidade Vehicle
     * @return VehicleDTO
     */
    public static VehicleDTO fromEntity(Vehicle vehicle) {
        return new VehicleDTO(vehicle);
    }
}
