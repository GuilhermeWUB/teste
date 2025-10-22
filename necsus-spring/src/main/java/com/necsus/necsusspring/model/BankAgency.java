package com.necsus.necsusspring.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bank_agency")
public class BankAgency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agency_code")
    private String agencyCode;

    @Column(name = "agency_dv")
    private String agencyDv;

    @ManyToOne
    @JoinColumn(name = "banks_id")
    private Bank bank;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgencyCode() {
        return agencyCode;
    }

    public void setAgencyCode(String agencyCode) {
        this.agencyCode = agencyCode;
    }

    public String getAgencyDv() {
        return agencyDv;
    }

    public void setAgencyDv(String agencyDv) {
        this.agencyDv = agencyDv;
    }

    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }
}