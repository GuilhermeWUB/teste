package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Adhesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "partners_id")
    @ToString.Exclude          // <--- OBRIGATÓRIO: Quebra o ciclo com Partner
    @EqualsAndHashCode.Exclude // <--- OBRIGATÓRIO: Quebra o ciclo com Partner
    private Partner partner;

    @NotNull(message = "O valor da adesão é obrigatório")
    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "vencimento")
    @Temporal(TemporalType.DATE)
    private Date vencimento;

}