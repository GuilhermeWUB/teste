package com.necsus.necsusspring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import jakarta.persistence.OneToMany;
import java.util.List;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

@Entity
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "O nome é obrigatório")
    private String name;
    private LocalDate dateBorn;
    private String zipcode;
    private String address;
    private String neighborhood;
    private String numResid;
    private String complement;
    @NotEmpty(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;
    private String city;
    private String uf;
    @NotEmpty(message = "O CPF é obrigatório")
    private String cpf;
    private String phone;
    private String cell;
    private String rg;
    private String fax;
    private Long addressId;

    @OneToMany(mappedBy = "partner")
    private List<Vehicle> vehicles;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;


    public @NotEmpty(message = "O nome é obrigatório") String getName() {
        return name;
    }

    public void setName(@NotEmpty(message = "O nome é obrigatório") String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateBorn() {
        return dateBorn;
    }

    public void setDateBorn(LocalDate dateBorn) {
        this.dateBorn = dateBorn;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getNumResid() {
        return numResid;
    }

    public void setNumResid(String numResid) {
        this.numResid = numResid;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public @NotEmpty(message = "O e-mail é obrigatório") @Email(message = "E-mail inválido") String getEmail() {
        return email;
    }

    public void setEmail(@NotEmpty(message = "O e-mail é obrigatório") @Email(message = "E-mail inválido") String email) {
        this.email = email;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public @NotEmpty(message = "O CPF é obrigatório") String getCpf() {
        return cpf;
    }

    public void setCpf(@NotEmpty(message = "O CPF é obrigatório") String cpf) {
        this.cpf = cpf;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCell() {
        return cell;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }

    public String getRg() {
        return rg;
    }

    public void setRg(String rg) {
        this.rg = rg;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
