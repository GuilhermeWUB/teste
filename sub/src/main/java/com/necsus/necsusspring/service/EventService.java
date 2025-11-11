package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.EventBoardCardDto;
import com.necsus.necsusspring.dto.EventBoardSnapshot;
import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.model.Status;
import com.necsus.necsusspring.model.Prioridade;
import com.necsus.necsusspring.model.Motivo;
import com.necsus.necsusspring.model.Envolvimento;
import com.necsus.necsusspring.repository.EventRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final PartnerRepository partnerRepository;
    private final VehicleRepository vehicleRepository;
    private final EventObservationHistoryService observationHistoryService;
    private final EventDescriptionHistoryService descriptionHistoryService;

    public EventService(EventRepository eventRepository,
                        PartnerRepository partnerRepository,
                        VehicleRepository vehicleRepository,
                        EventObservationHistoryService observationHistoryService,
                        EventDescriptionHistoryService descriptionHistoryService) {
        this.eventRepository = eventRepository;
        this.partnerRepository = partnerRepository;
        this.vehicleRepository = vehicleRepository;
        this.observationHistoryService = observationHistoryService;
        this.descriptionHistoryService = descriptionHistoryService;
    }

    @Transactional(readOnly = true)
    public List<Event> listAll() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Event> listAllWithRelations() {
        return eventRepository.findAllByOrderByStatusAscDataVencimentoAscIdAsc();
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

    @Transactional(readOnly = true)
    public EventBoardSnapshot getBoardSnapshot() {
        List<Event> events = listAllWithRelations();
        List<EventBoardCardDto> cards = events.stream()
                .map(EventBoardCardDto::from)
                .toList();

        LinkedHashMap<String, List<EventBoardCardDto>> grouped = new LinkedHashMap<>();
        for (Status status : Status.values()) {
            grouped.put(status.name(), new ArrayList<>());
        }

        cards.forEach(card -> grouped
                .computeIfAbsent(card.status(), key -> new ArrayList<>())
                .add(card));

        Comparator<EventBoardCardDto> comparator = Comparator
                .comparing(EventBoardCardDto::dataVencimento, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EventBoardCardDto::titulo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(EventBoardCardDto::id, Comparator.nullsLast(Long::compareTo));

        grouped.values().forEach(list -> list.sort(comparator));

        LinkedHashMap<String, List<EventBoardCardDto>> immutableGrouped = new LinkedHashMap<>();
        grouped.forEach((status, list) -> immutableGrouped.put(status, List.copyOf(list)));

        LinkedHashMap<String, Long> counters = new LinkedHashMap<>();
        immutableGrouped.forEach((status, list) -> counters.put(status, (long) list.size()));

        return new EventBoardSnapshot(
                List.copyOf(cards),
                Collections.unmodifiableMap(immutableGrouped),
                Collections.unmodifiableMap(counters)
        );
    }

    @Transactional
    public Event create(Event event) {
        // Se status não foi definido, define como COMUNICADO por padrão
        if (event.getStatus() == null) {
            event.setStatus(Status.COMUNICADO);
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
        existing.setPlacaManual(eventPayload.getPlacaManual());
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
    public Event updatePartial(Long id, java.util.Map<String, Object> updates) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));

        // Atualiza apenas os campos fornecidos
        updates.forEach((key, value) -> {
            switch (key) {
                case "titulo":
                    if (value != null) existing.setTitulo(value.toString());
                    break;
                case "descricao":
                    existing.setDescricao(value != null ? value.toString() : null);
                    break;
                case "status":
                    if (value != null) existing.setStatus(Status.valueOf(value.toString()));
                    break;
                case "prioridade":
                    existing.setPrioridade(value != null ? Prioridade.valueOf(value.toString()) : null);
                    break;
                case "dataVencimento":
                    existing.setDataVencimento(value != null ? LocalDate.parse(value.toString()) : null);
                    break;
                case "analistaResponsavel":
                    existing.setAnalistaResponsavel(value != null ? value.toString() : null);
                    break;
                case "observacoes":
                    existing.setObservacoes(value != null ? value.toString() : null);
                    break;
                case "placaManual":
                    existing.setPlacaManual(value != null ? value.toString() : null);
                    break;
            }
        });

        return eventRepository.save(existing);
    }

    /**
     * Atualização parcial com rastreamento de histórico de observações e descrições
     */
    @Transactional
    public Event updatePartialWithHistory(Long id, java.util.Map<String, Object> updates, String modifiedBy) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));

        // Captura valores anteriores antes de fazer updates
        String previousObservation = existing.getObservacoes();
        String previousDescription = existing.getDescricao();

        // Atualiza apenas os campos fornecidos
        updates.forEach((key, value) -> {
            switch (key) {
                case "titulo":
                    if (value != null) existing.setTitulo(value.toString());
                    break;
                case "descricao":
                    existing.setDescricao(value != null ? value.toString() : null);
                    break;
                case "status":
                    if (value != null) existing.setStatus(Status.valueOf(value.toString()));
                    break;
                case "prioridade":
                    existing.setPrioridade(value != null ? Prioridade.valueOf(value.toString()) : null);
                    break;
                case "dataVencimento":
                    existing.setDataVencimento(value != null ? LocalDate.parse(value.toString()) : null);
                    break;
                case "analistaResponsavel":
                    existing.setAnalistaResponsavel(value != null ? value.toString() : null);
                    break;
                case "observacoes":
                    existing.setObservacoes(value != null ? value.toString() : null);
                    break;
                case "placaManual":
                    existing.setPlacaManual(value != null ? value.toString() : null);
                    break;
            }
        });

        Event savedEvent = eventRepository.save(existing);

        // Registra histórico se observação foi alterada e modifiedBy foi fornecido
        if (modifiedBy != null && updates.containsKey("observacoes")) {
            observationHistoryService.recordChange(savedEvent, previousObservation,
                savedEvent.getObservacoes(), modifiedBy);
        }

        // Registra histórico se descrição foi alterada e modifiedBy foi fornecido
        if (modifiedBy != null && updates.containsKey("descricao")) {
            descriptionHistoryService.recordChange(savedEvent, previousDescription,
                savedEvent.getDescricao(), modifiedBy);
        }

        return savedEvent;
    }

    /**
     * Atualiza o evento e registra histórico de mudanças nas observações e descrições
     */
    @Transactional
    public Event updateWithHistory(Long id, Event eventPayload, String modifiedBy) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id " + id));

        // Captura valores anteriores para histórico
        String previousObservation = existing.getObservacoes();
        String previousDescription = existing.getDescricao();

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
        existing.setPlacaManual(eventPayload.getPlacaManual());
        existing.setPartner(eventPayload.getPartner());
        existing.setVehicle(eventPayload.getVehicle());

        Event savedEvent = eventRepository.save(existing);

        // Registra histórico se observação foi alterada
        if (modifiedBy != null) {
            observationHistoryService.recordChange(savedEvent, previousObservation, eventPayload.getObservacoes(), modifiedBy);
            descriptionHistoryService.recordChange(savedEvent, previousDescription, eventPayload.getDescricao(), modifiedBy);
        }

        return savedEvent;
    }

    @Transactional
    public void delete(Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
        }
    }
}