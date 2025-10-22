package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}