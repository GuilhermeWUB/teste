package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Vehicle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByPartnerId(Long partnerId);

    @EntityGraph(attributePaths = {"partner", "payment"})
    Optional<Vehicle> findWithPartnerAndPaymentById(Long id);

    long countByStatus(Integer status);

    Optional<Vehicle> findByPartnerIdAndPlaque(Long partnerId, String plaque);
}
