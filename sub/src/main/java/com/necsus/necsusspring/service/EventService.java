package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Status;
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
    public List<Event> listByStatus(Status status) {
        return eventRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Event> listByPartnerId(Long partnerId) {
        return eventRepository.findByPartnerId(partnerId);
    }

    @Transactional(readOnly = true)
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    @Transactional
    public Event create(Event event) {
        // Se status não foi definido, define como A_FAZER por padrão
        if (event.getStatus() == null) {
            event.setStatus(Status.A_FAZER);
        }
        return eventRepository.save(event);
    }

    @Transactional
    public Event update(Long id, Event eventPayload) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));

        existing.setTitulo(eventPayload.getTitulo());
        existing.setDescricao(eventPayload.getDescricao());
        existing.setStatus(eventPayload.getStatus());
        existing.setPrioridade(eventPayload.getPrioridade());
        existing.setMotivo(eventPayload.getMotivo());
        existing.setEnvolvimento(eventPayload.getEnvolvimento());
        existing.setDataAconteceu(eventPayload.getDataAconteceu());
        existing.setHoraAconteceu(eventPayload.getHoraAconteceu());
        existing.setDataComunicacao(eventPayload.getDataComunicacao());
        existing.setHoraComunicacao(eventPayload.getHoraComunicacao());
        existing.setDataVencimento(eventPayload.getDataVencimento());
        existing.setObservacoes(eventPayload.getObservacoes());
        existing.setIdExterno(eventPayload.getIdExterno());
        existing.setAnalistaResponsavel(eventPayload.getAnalistaResponsavel());
        existing.setPartner(eventPayload.getPartner());
        existing.setVehicle(eventPayload.getVehicle());

        return eventRepository.save(existing);
    }

    @Transactional
    public Event updateStatus(Long id, Status newStatus) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));

        existing.setStatus(newStatus);
        return eventRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
        }
    }
}