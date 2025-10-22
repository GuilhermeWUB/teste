package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.BankShipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankShipmentRepository extends JpaRepository<BankShipment, Long> {
}