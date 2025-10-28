package com.necsus.necsusspring.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Email;
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
}
