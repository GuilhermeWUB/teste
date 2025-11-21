package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    private String cnpj;
    private String phone;
    private String cell;
    private String rg;
    private String fax;

    @Enumerated(EnumType.STRING)
    private PartnerStatus status;

    @ElementCollection
    private List<String> documentPaths = new ArrayList<>();

    private LocalDateTime registrationDate;
    private LocalDate contractDate;

    @PrePersist
    protected void onCreate() {
        if (registrationDate == null) {
            registrationDate = LocalDateTime.now();
        }
    }

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    @ToString.Exclude          // <--- OBRIGATÓRIO
    @EqualsAndHashCode.Exclude // <--- OBRIGATÓRIO
    private Address address;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude          // <--- OBRIGATÓRIO
    @EqualsAndHashCode.Exclude // <--- OBRIGATÓRIO
    private List<Vehicle> vehicles;

    @OneToOne(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude          // <--- OBRIGATÓRIO (Aqui estava o loop principal)
    @EqualsAndHashCode.Exclude // <--- OBRIGATÓRIO
    private Adhesion adhesion;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude          // <--- OBRIGATÓRIO
    @EqualsAndHashCode.Exclude // <--- OBRIGATÓRIO
    private Set<BankSlip> bankSlips = new LinkedHashSet<>();

    @ManyToOne
    @JoinColumn(name = "company_id")
    @ToString.Exclude          // <--- OBRIGATÓRIO
    @EqualsAndHashCode.Exclude // <--- OBRIGATÓRIO
    private Company company;
}