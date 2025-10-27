package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.BankAgency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAgencyRepository extends JpaRepository<BankAgency, Long> {
}