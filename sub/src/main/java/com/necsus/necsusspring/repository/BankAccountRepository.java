package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
}