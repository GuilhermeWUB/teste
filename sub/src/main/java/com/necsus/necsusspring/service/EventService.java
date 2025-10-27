package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<Event> listAll() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    @Transactional
    public Event create(Event event) {
        return eventRepository.save(event);
    }

    @Transactional
    public Event update(Long id, Event eventPayload) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));

        existing.setMotivo(eventPayload.getMotivo());
        existing.setEnvolvimento(eventPayload.getEnvolvimento());
        existing.setDataAconteceu(eventPayload.getDataAconteceu());
        existing.setHoraAconteceu(eventPayload.getHoraAconteceu());
        existing.setDataComunicacao(eventPayload.getDataComunicacao());
        existing.setHoraComunicacao(eventPayload.getHoraComunicacao());
        existing.setIdExterno(eventPayload.getIdExterno());
        existing.setAnalistaResponsavel(eventPayload.getAnalistaResponsavel());

        return eventRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
        }
    }
}
