package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.BankSlip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankSlipRepository extends JpaRepository<BankSlip, Long> {

    long countByStatus(Integer status);
}
