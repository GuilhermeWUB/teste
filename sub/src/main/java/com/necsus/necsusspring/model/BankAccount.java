package com.necsus.necsusspring.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bank_account")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code")
    private String accountCode;

    @Column(name = "account_dv")
    private String accountDv;

    @ManyToOne
    @JoinColumn(name = "bank_agency_id")
    private BankAgency bankAgency;

    @ManyToOne
    @JoinColumn(name = "companies_id")
    private Company company;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountDv() {
        return accountDv;
    }

    public void setAccountDv(String accountDv) {
        this.accountDv = accountDv;
    }

    public BankAgency getBankAgency() {
        return bankAgency;
    }

    public void setBankAgency(BankAgency bankAgency) {
        this.bankAgency = bankAgency;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}