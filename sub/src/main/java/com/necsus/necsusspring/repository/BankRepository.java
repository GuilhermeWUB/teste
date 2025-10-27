package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepository extends JpaRepository<Bank, Long> {
}