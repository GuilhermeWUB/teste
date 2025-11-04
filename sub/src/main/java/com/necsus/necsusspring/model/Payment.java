package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "info_payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "vehicle_id", unique = true)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "partners_id")
    private Partner partner;

    @Column(name = "monthly")
    @NotNull(message = "Informe o valor da mensalidade")
    @Positive(message = "A mensalidade deve ser maior que zero")
    private BigDecimal monthly;

    @Column(name = "vencimento")
    @NotNull(message = "Informe o dia do vencimento")
    @Min(value = 1, message = "O vencimento deve ser entre 1 e 31")
    @Max(value = 31, message = "O vencimento deve ser entre 1 e 31")
    private Integer vencimento;

    @Column(name = "date_create")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreate;
}
