package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.model.Status;
import com.necsus.necsusspring.repository.EventRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final PartnerRepository partnerRepository;
    private final VehicleRepository vehicleRepository;

    public EventService(EventRepository eventRepository,
                        PartnerRepository partnerRepository,
                        VehicleRepository vehicleRepository) {
        this.eventRepository = eventRepository;
        this.partnerRepository = partnerRepository;
        this.vehicleRepository = vehicleRepository;
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
        // Resolver referências de Partner e Vehicle a partir dos IDs enviados pelo formulário
        if (event.getPartner() != null && event.getPartner().getId() != null) {
            Partner partner = partnerRepository.findById(event.getPartner().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Partner não encontrado"));
            event.setPartner(partner);
        }
        if (event.getVehicle() != null && event.getVehicle().getId() != null) {
            Vehicle vehicle = vehicleRepository.findById(event.getVehicle().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle não encontrado"));
            event.setVehicle(vehicle);
        } else if (event.getVehicle() != null && event.getPartner() != null
                && event.getPartner().getId() != null
                && event.getVehicle().getPlaque() != null && !event.getVehicle().getPlaque().isBlank()) {
            // Fallback: localizar pelo par (partnerId, plaque) quando o id não veio do formulário
            Vehicle vehicle = vehicleRepository
                    .findByPartnerIdAndPlaque(event.getPartner().getId(), event.getVehicle().getPlaque())
                    .orElseThrow(() -> new IllegalArgumentException("Placa não encontrada para o associado informado"));
            event.setVehicle(vehicle);
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