package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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
    private Partner partner;

    @NotNull(message = "O valor da adesão é obrigatório")
    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "vencimento")
    @Temporal(TemporalType.DATE)
    private Date vencimento;

}
