package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByPartnerId(Long partnerId);
}
