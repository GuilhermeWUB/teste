package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Vistoria;
import com.necsus.necsusspring.repository.EventRepository;
import com.necsus.necsusspring.repository.VistoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VistoriaService {

    private final VistoriaRepository vistoriaRepository;
    private final EventRepository eventRepository;

    public VistoriaService(VistoriaRepository vistoriaRepository, EventRepository eventRepository) {
        this.vistoriaRepository = vistoriaRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<Vistoria> listAll() {
        return vistoriaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Vistoria> listByEventId(Long eventId) {
        return vistoriaRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public Optional<Vistoria> findById(Long id) {
        return vistoriaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Vistoria> findLatestByEventId(Long eventId) {
        return vistoriaRepository.findFirstByEventIdOrderByDataCriacaoDesc(eventId);
    }

    @Transactional(readOnly = true)
    public boolean existsByEventId(Long eventId) {
        return vistoriaRepository.existsByEventId(eventId);
    }

    @Transactional
    public Vistoria create(Vistoria vistoria) {
        // Valida se o evento existe
        if (vistoria.getEvent() != null && vistoria.getEvent().getId() != null) {
            Event event = eventRepository.findById(vistoria.getEvent().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado"));
            vistoria.setEvent(event);
        } else {
            throw new IllegalArgumentException("Evento é obrigatório para criar uma vistoria");
        }

        return vistoriaRepository.save(vistoria);
    }

    @Transactional
    public Vistoria update(Long id, Vistoria vistoriaPayload) {
        Vistoria existing = vistoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vistoria não encontrada com id " + id));

        // Atualiza os campos
        existing.setObservacoes(vistoriaPayload.getObservacoes());
        existing.setUsuarioCriacao(vistoriaPayload.getUsuarioCriacao());

        // Atualiza as fotos - remove todas as antigas e adiciona as novas
        if (vistoriaPayload.getFotos() != null && !vistoriaPayload.getFotos().isEmpty()) {
            existing.getFotos().clear();
            vistoriaPayload.getFotos().forEach(existing::adicionarFoto);
        }

        return vistoriaRepository.save(existing);
    }

    @Transactional
    public boolean delete(Long id) {
        Optional<Vistoria> existing = vistoriaRepository.findById(id);
        if (existing.isEmpty()) {
            return false;
        }

        vistoriaRepository.delete(existing.get());
        return true;
    }
}
