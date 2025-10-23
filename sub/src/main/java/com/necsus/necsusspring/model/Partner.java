package com.necsus.necsusspring.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.time.LocalDate;
import java.util.List;
import jakarta.persistence.OneToMany;
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
    @NotEmpty(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;
    @NotEmpty(message = "O CPF é obrigatório")
    private String cpf;
    private String phone;
    private String cell;
    private String rg;
    private String fax;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vehicle> vehicles;

    @OneToOne(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Adhesion adhesion;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BankSlip> bankSlips;

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

    public @NotEmpty(message = "O e-mail é obrigatório") @Email(message = "E-mail inválido") String getEmail() {
        return email;
    }

    public void setEmail(@NotEmpty(message = "O e-mail é obrigatório") @Email(message = "E-mail inválido") String email) {
        this.email = email;
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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
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

    public Adhesion getAdhesion() {
        return adhesion;
    }

    public void setAdhesion(Adhesion adhesion) {
        this.adhesion = adhesion;
    }

    public List<BankSlip> getBankSlips() {
        return bankSlips;
    }

    public void setBankSlips(List<BankSlip> bankSlips) {
        this.bankSlips = bankSlips;
    }
}
