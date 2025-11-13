package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.LegalProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LegalProcessRepository extends JpaRepository<LegalProcess, Long> {
    Optional<LegalProcess> findByNumeroProcesso(String numeroProcesso);

    @Query("select lp.sourceEventId from LegalProcess lp where lp.sourceEventId is not null")
    List<Long> findAllSourceEventIds();
}
