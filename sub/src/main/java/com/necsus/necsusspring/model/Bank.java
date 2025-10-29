package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "banks")
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

}