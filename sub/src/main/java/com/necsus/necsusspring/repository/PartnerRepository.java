package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Partner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {
    Optional<Partner> findByEmailIgnoreCase(String email);

    @Query("SELECT DISTINCT p FROM Partner p " +
           "LEFT JOIN FETCH p.address " +
           "LEFT JOIN FETCH p.adhesion " +
           "LEFT JOIN FETCH p.vehicles v " +
           "LEFT JOIN FETCH v.payment " +
           "WHERE p.id = :id")
    Optional<Partner> findByIdWithAllRelationships(@Param("id") Long id);

    @Query(value = "SELECT DISTINCT p FROM Partner p " +
           "LEFT JOIN FETCH p.vehicles",
           countQuery = "SELECT COUNT(p) FROM Partner p")
    Page<Partner> findAllWithVehicles(Pageable pageable);

    @Query(value = "SELECT DISTINCT p FROM Partner p " +
           "LEFT JOIN FETCH p.vehicles " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR REPLACE(p.cpf, '.', '') LIKE CONCAT('%', REPLACE(REPLACE(:searchTerm, '.', ''), '-', ''), '%')",
           countQuery = "SELECT COUNT(p) FROM Partner p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR REPLACE(p.cpf, '.', '') LIKE CONCAT('%', REPLACE(REPLACE(:searchTerm, '.', ''), '-', ''), '%')")
    Page<Partner> searchByNameOrCpf(@Param("searchTerm") String searchTerm, Pageable pageable);
}
