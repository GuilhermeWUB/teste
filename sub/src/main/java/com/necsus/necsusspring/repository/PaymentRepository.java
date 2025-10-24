package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByVehicleId(Long vehicleId);
    void deleteByVehicleId(Long vehicleId);
}