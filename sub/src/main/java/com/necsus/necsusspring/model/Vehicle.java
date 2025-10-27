package com.necsus.necsusspring.model;

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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Entity
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
    @Valid
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "partners_id", insertable = false, updatable = false)
    private Partner partner;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaker() {
        return maker;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }

    public String getType_vehicle() {
        return type_vehicle;
    }

    public void setType_vehicle(String type_vehicle) {
        this.type_vehicle = type_vehicle;
    }

    public String getPlaque() {
        return plaque;
    }

    public void setPlaque(String plaque) {
        this.plaque = plaque;
    }

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getChassis() {
        return chassis;
    }

    public void setChassis(String chassis) {
        this.chassis = chassis;
    }

    public Integer getPorts() {
        return ports;
    }

    public void setPorts(Integer ports) {
        this.ports = ports;
    }

    public String getYear_mod() {
        return year_mod;
    }

    public void setYear_mod(String year_mod) {
        this.year_mod = year_mod;
    }

    public String getYear_maker() {
        return year_maker;
    }

    public void setYear_maker(String year_maker) {
        this.year_maker = year_maker;
    }

    public Date getDate_dut() {
        return date_dut;
    }

    public void setDate_dut(Date date_dut) {
        this.date_dut = date_dut;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getKm_vehicle() {
        return km_vehicle;
    }

    public void setKm_vehicle(String km_vehicle) {
        this.km_vehicle = km_vehicle;
    }

    public String getExpedition() {
        return expedition;
    }

    public void setExpedition(String expedition) {
        this.expedition = expedition;
    }

    public String getDut_named_to() {
        return dut_named_to;
    }

    public void setDut_named_to(String dut_named_to) {
        this.dut_named_to = dut_named_to;
    }

    public String getRenavam() {
        return renavam;
    }

    public void setRenavam(String renavam) {
        this.renavam = renavam;
    }

    public Double getFipe_value() {
        return fipe_value;
    }

    public void setFipe_value(Double fipe_value) {
        this.fipe_value = fipe_value;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getStates_id() {
        return states_id;
    }

    public void setStates_id(Long states_id) {
        this.states_id = states_id;
    }

    public String getBreakdowns() {
        return breakdowns;
    }

    public void setBreakdowns(String breakdowns) {
        this.breakdowns = breakdowns;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public Date getContract_begin() {
        return contract_begin;
    }

    public void setContract_begin(Date contract_begin) {
        this.contract_begin = contract_begin;
    }

    public Date getContract_end() {
        return contract_end;
    }

    public void setContract_end(Date contract_end) {
        this.contract_end = contract_end;
    }

    public String getCodigo_externo() {
        return codigo_externo;
    }

    public void setCodigo_externo(String codigo_externo) {
        this.codigo_externo = codigo_externo;
    }

    public String getCodigo_fipe() {
        return codigo_fipe;
    }

    public void setCodigo_fipe(String codigo_fipe) {
        this.codigo_fipe = codigo_fipe;
    }

    public String getTipo_combustivel() {
        return tipo_combustivel;
    }

    public void setTipo_combustivel(String tipo_combustivel) {
        this.tipo_combustivel = tipo_combustivel;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Partner getPartner() {
        return partner;
    }

    public void setPartner(Partner partner) {
        this.partner = partner;
    }

    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(VehicleStatus vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public List<String> getInspectionPhotoPaths() {
        return inspectionPhotoPaths;
    }

    public void setInspectionPhotoPaths(List<String> inspectionPhotoPaths) {
        this.inspectionPhotoPaths = inspectionPhotoPaths;
    }

    public List<String> getDocumentPhotoPaths() {
        return documentPhotoPaths;
    }

    public void setDocumentPhotoPaths(List<String> documentPhotoPaths) {
        this.documentPhotoPaths = documentPhotoPaths;
    }
}
