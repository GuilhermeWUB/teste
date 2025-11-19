package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Agreement;
import com.necsus.necsusspring.model.AgreementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    List<Agreement> findByStatus(AgreementStatus status);

    List<Agreement> findByNumeroProcesso(String numeroProcesso);

    List<Agreement> findByParteEnvolvidaContainingIgnoreCase(String parteEnvolvida);

    List<Agreement> findAllByOrderByCreatedAtDesc();
}
