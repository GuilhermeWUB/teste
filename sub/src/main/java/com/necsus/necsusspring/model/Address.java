package com.necsus.necsusspring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    // Getter customizado para manter compatibilidade
    public String getEndereco() {
        return address;
    }

    public String getBairro() {
        return neighborhood;
    }

    public String getCidade() {
        return city;
    }

    public String getComplemento() {
        return complement;
    }

    public String getCep() {
        return zipcode;
    }

    public String getNumero() {
        return number;
    }

    public String getEstado() {
        return states;
    }
}
