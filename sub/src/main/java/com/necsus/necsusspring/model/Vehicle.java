package com.necsus.necsusspring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ElementCollection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "payment", "partner"})
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe a montadora")
    private String maker;
    private String type_vehicle;
    @NotBlank(message = "Informe a placa")
    private String plaque;

    @Column(name = "partners_id")
    @NotNull(message = "Selecione o associado")
    private Long partnerId;

    @NotBlank(message = "Informe o modelo")
    private String model;
    private Integer status;

    @Enumerated(EnumType.STRING)
    private VehicleStatus vehicleStatus;

    @ElementCollection
    private List<String> inspectionPhotoPaths = new ArrayList<>();

    @ElementCollection
    private List<String> documentPhotoPaths = new ArrayList<>();

    @NotBlank(message = "Informe a cor")
    private String color;
    private String chassis;
    private Integer ports;
    @NotBlank(message = "Informe o ano/modelo")
    private String year_mod;
    private String year_maker;
    private Date date_dut;
    private String category;
    private String km_vehicle;
    private String expedition;
    private String dut_named_to;
    private String renavam;
    private Double fipe_value;
    private String notes;
    private Long states_id;
    private String breakdowns;
    private String transmission;
    private Date contract_begin;
    private Date contract_end;
    private String codigo_externo;
    private String codigo_fipe;
    @NotBlank(message = "Informe o tipo de combustível")
    private String tipo_combustivel;

    @OneToOne(mappedBy = "vehicle")
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "partners_id", insertable = false, updatable = false)
    private Partner partner;
}
