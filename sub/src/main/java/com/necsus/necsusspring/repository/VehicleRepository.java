package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {
    List<Vehicle> findByPartnerId(Long partnerId);

    @EntityGraph(attributePaths = {"partner", "payment"})
    Optional<Vehicle> findWithPartnerAndPaymentById(Long id);

    long countByStatus(Integer status);

    Optional<Vehicle> findByPartnerIdAndPlaque(Long partnerId, String plaque);

    @Query("SELECT v FROM Vehicle v " +
           "WHERE LOWER(v.maker) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(v.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(v.plaque) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Vehicle> searchByMakerModelOrPlaque(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT v FROM Vehicle v " +
           "WHERE (LOWER(v.maker) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(v.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(v.plaque) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND v.partnerId = :partnerId")
    Page<Vehicle> searchByMakerModelOrPlaqueAndPartner(@Param("searchTerm") String searchTerm,
                                                        @Param("partnerId") Long partnerId,
                                                        Pageable pageable);
}
