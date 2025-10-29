package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Entity
@Data
@Table(name = "bank_shipment")
public class BankShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "date_create")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreate;

    @Column(name = "status")
    private Integer status;

    @OneToMany(mappedBy = "bankShipment")
    private List<BankSlip> bankSlips;

}