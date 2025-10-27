package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}