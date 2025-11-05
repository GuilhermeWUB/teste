package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"partner", "vehicle"})
    List<Event> findByStatus(Status status);

    List<Event> findByPartnerId(Long partnerId);

    List<Event> findByVehicleId(Long vehicleId);

    void deleteByVehicleId(Long vehicleId);

    @EntityGraph(attributePaths = {"partner", "vehicle"})
    List<Event> findAllByOrderByStatusAscDataVencimentoAscIdAsc();
}