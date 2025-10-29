package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
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

}