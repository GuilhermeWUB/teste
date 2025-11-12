package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.LegalProcess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LegalProcessRepository extends JpaRepository<LegalProcess, Long> {
    Optional<LegalProcess> findByNumeroProcesso(String numeroProcesso);
}
