package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.EventObservationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventObservationHistoryRepository extends JpaRepository<EventObservationHistory, Long> {

    List<EventObservationHistory> findByEventIdOrderByModifiedAtDesc(Long eventId);

    void deleteByEventId(Long eventId);
}
