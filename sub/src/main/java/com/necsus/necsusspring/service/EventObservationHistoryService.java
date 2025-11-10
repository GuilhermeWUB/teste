package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.EventObservationHistory;
import com.necsus.necsusspring.repository.EventObservationHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventObservationHistoryService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EventObservationHistoryService.class);

    private final EventObservationHistoryRepository historyRepository;

    public EventObservationHistoryService(EventObservationHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void recordChange(Event event, String previousObservation, String newObservation, String modifiedBy) {
        // Só registra se houve mudança real
        if (isObservationChanged(previousObservation, newObservation)) {
            EventObservationHistory history = new EventObservationHistory(
                    event,
                    previousObservation,
                    newObservation,
                    modifiedBy
            );

            historyRepository.save(history);
            logger.info("Histórico de observação registrado para evento {} por {}", event.getId(), modifiedBy);
        }
    }

    public List<EventObservationHistory> getHistoryByEventId(Long eventId) {
        return historyRepository.findByEventIdOrderByModifiedAtDesc(eventId);
    }

    private boolean isObservationChanged(String oldValue, String newValue) {
        // Trata null e string vazia como equivalentes
        String normalizedOld = (oldValue == null || oldValue.trim().isEmpty()) ? null : oldValue.trim();
        String normalizedNew = (newValue == null || newValue.trim().isEmpty()) ? null : newValue.trim();

        if (normalizedOld == null && normalizedNew == null) {
            return false;
        }

        if (normalizedOld == null || normalizedNew == null) {
            return true;
        }

        return !normalizedOld.equals(normalizedNew);
    }
}
