package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.EventDescriptionHistory;
import com.necsus.necsusspring.repository.EventDescriptionHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventDescriptionHistoryService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EventDescriptionHistoryService.class);

    private final EventDescriptionHistoryRepository historyRepository;

    public EventDescriptionHistoryService(EventDescriptionHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void recordChange(Event event, String previousDescription, String newDescription, String modifiedBy) {
        // Só registra se houve mudança real
        if (isDescriptionChanged(previousDescription, newDescription)) {
            EventDescriptionHistory history = new EventDescriptionHistory(
                    event,
                    previousDescription,
                    newDescription,
                    modifiedBy
            );

            historyRepository.save(history);
            logger.info("Histórico de descrição registrado para evento {} por {}", event.getId(), modifiedBy);
        }
    }

    public List<EventDescriptionHistory> getHistoryByEventId(Long eventId) {
        return historyRepository.findByEventIdOrderByModifiedAtDesc(eventId);
    }

    @Transactional
    public void deleteByEventId(Long eventId) {
        historyRepository.deleteByEventId(eventId);
        logger.info("Histórico de descrições removido para evento {}", eventId);
    }

    private boolean isDescriptionChanged(String oldValue, String newValue) {
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
