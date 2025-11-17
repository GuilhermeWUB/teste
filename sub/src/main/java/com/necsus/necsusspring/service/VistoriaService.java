package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Vistoria;
import com.necsus.necsusspring.repository.EventRepository;
import com.necsus.necsusspring.repository.VistoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VistoriaService {

    private static final Logger logger = LoggerFactory.getLogger(VistoriaService.class);

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
    public Optional<Vistoria> findLatestWithPhotosByEventId(Long eventId) {
        return vistoriaRepository.findFirstByEventIdOrderByDataCriacaoDesc(eventId)
                .map(v -> {
                    if (v.getFotos() != null) {
                        v.getFotos().size();
                    }
                    return v;
                });
    }

    @Transactional(readOnly = true)
    public boolean existsByEventId(Long eventId) {
        return vistoriaRepository.existsByEventId(eventId);
    }

    @Transactional
    public Vistoria create(Vistoria vistoria) {
        logger.info("=== CREATE VISTORIA SERVICE ===");
        logger.info("Fotos antes de salvar: {}", vistoria.getQuantidadeFotos());

        // Valida se o evento existe
        if (vistoria.getEvent() != null && vistoria.getEvent().getId() != null) {
            Event event = eventRepository.findById(vistoria.getEvent().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado"));
            vistoria.setEvent(event);
        } else {
            throw new IllegalArgumentException("Evento é obrigatório para criar uma vistoria");
        }

        Vistoria saved = vistoriaRepository.save(vistoria);
        logger.info("Vistoria salva com ID: {}, Fotos: {}", saved.getId(), saved.getQuantidadeFotos());
        return saved;
    }

    @Transactional
    public Vistoria update(Long id, Vistoria vistoriaPayload) {
        logger.info("=== UPDATE VISTORIA SERVICE ===");
        logger.info("Vistoria ID: {}", id);
        logger.info("Fotos no payload: {}", vistoriaPayload.getQuantidadeFotos());

        Vistoria existing = vistoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vistoria não encontrada com id " + id));

        logger.info("Fotos na vistoria existente (antes): {}", existing.getFotos().size());

        // Atualiza os campos
        existing.setObservacoes(vistoriaPayload.getObservacoes());
        existing.setUsuarioCriacao(vistoriaPayload.getUsuarioCriacao());

        // Atualiza as fotos - adiciona as novas mantendo as antigas
        if (vistoriaPayload.getFotos() != null && !vistoriaPayload.getFotos().isEmpty()) {
            logger.info("Adicionando {} novas fotos", vistoriaPayload.getFotos().size());
            vistoriaPayload.getFotos().forEach(foto -> {
                logger.info("Adicionando foto: path={}, ordem={}", foto.getFotoPath(), foto.getOrdem());
                existing.adicionarFoto(foto);
            });
        } else {
            logger.warn("Nenhuma foto nova para adicionar");
        }

        logger.info("Fotos na vistoria existente (depois): {}", existing.getFotos().size());

        Vistoria saved = vistoriaRepository.save(existing);
        logger.info("Vistoria salva com ID: {}, Fotos: {}", saved.getId(), saved.getQuantidadeFotos());
        return saved;
    }

    /**
     * Atualiza vistoria com novas fotos (método que evita ConcurrentModificationException)
     */
    @Transactional
    public Vistoria updateWithNewPhotos(Long id, String observacoes, List<com.necsus.necsusspring.model.VistoriaFoto> novasFotos) {
        logger.info("=== UPDATE VISTORIA WITH NEW PHOTOS ===");
        logger.info("Vistoria ID: {}", id);
        logger.info("Novas fotos recebidas: {}", novasFotos != null ? novasFotos.size() : 0);

        Vistoria existing = vistoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vistoria não encontrada com id " + id));

        logger.info("Fotos existentes antes: {}", existing.getFotos().size());

        // Atualiza observações
        existing.setObservacoes(observacoes);

        // Adiciona as novas fotos
        if (novasFotos != null && !novasFotos.isEmpty()) {
            logger.info("Adicionando {} novas fotos", novasFotos.size());
            novasFotos.forEach(foto -> {
                logger.info("Adicionando foto: path={}, ordem={}", foto.getFotoPath(), foto.getOrdem());
                existing.adicionarFoto(foto);
            });
        }

        logger.info("Fotos totais depois: {}", existing.getFotos().size());

        Vistoria saved = vistoriaRepository.save(existing);
        logger.info("Vistoria salva com ID: {}, Fotos: {}", saved.getId(), saved.getQuantidadeFotos());
        return saved;
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
