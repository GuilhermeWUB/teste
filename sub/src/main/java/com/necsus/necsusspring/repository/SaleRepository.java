package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Sale;
import com.necsus.necsusspring.model.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByStatus(SaleStatus status);

    List<Sale> findByNomeContatoContainingIgnoreCase(String nomeContato);

    List<Sale> findAllByOrderByCreatedAtDesc();
}
