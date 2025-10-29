package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
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

}