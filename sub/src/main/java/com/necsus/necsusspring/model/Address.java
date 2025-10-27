package com.necsus.necsusspring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;

@Entity
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "O endereço é obrigatório")
    private String address;
    private String neighborhood;
    private String city;
    private String complement;
    @NotEmpty(message = "O CEP é obrigatório")
    private String zipcode;
    private String number;
    private String states;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotEmpty(message = "O endereço é obrigatório") String getAddress() {
        return address;
    }

    public void setAddress(@NotEmpty(message = "O endereço é obrigatório") String address) {
        this.address = address;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public @NotEmpty(message = "O CEP é obrigatório") String getZipcode() {
        return zipcode;
    }

    public void setZipcode(@NotEmpty(message = "O CEP é obrigatório") String zipcode) {
        this.zipcode = zipcode;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getStates() {
        return states;
    }

    public void setStates(String states) {
        this.states = states;
    }
}
