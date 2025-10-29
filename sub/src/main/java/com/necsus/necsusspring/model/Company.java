package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import java.util.List;

import lombok.*;

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "cnpj")
    private String cnpj;

    @OneToMany(mappedBy = "company")
    private List<BankAccount> bankAccounts;
}
