package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {
    Optional<Partner> findByEmailIgnoreCase(String email);
}
