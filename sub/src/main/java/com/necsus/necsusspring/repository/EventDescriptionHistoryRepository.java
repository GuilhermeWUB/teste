package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.EventDescriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventDescriptionHistoryRepository extends JpaRepository<EventDescriptionHistory, Long> {

    /**
     * Busca o histórico de alterações de descrição de um evento específico,
     * ordenado da mais recente para a mais antiga
     */
    List<EventDescriptionHistory> findByEventIdOrderByModifiedAtDesc(Long eventId);
}
