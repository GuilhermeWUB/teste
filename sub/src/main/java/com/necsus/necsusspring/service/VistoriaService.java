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

        Vistoria existing = vistoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vistoria não encontrada com id " + id));

        existing.setObservacoes(vistoriaPayload.getObservacoes());
        existing.setUsuarioCriacao(vistoriaPayload.getUsuarioCriacao());

        if (vistoriaPayload.getAnaliseIa() != null) {
            existing.setAnaliseIa(vistoriaPayload.getAnaliseIa());
        }

        // Lógica segura para adicionar fotos SEM duplicar ou causar erro de concorrência
        // Só adicionamos se o payload tiver fotos NOVAS que ainda não estão no banco
        if (vistoriaPayload.getFotos() != null && !vistoriaPayload.getFotos().isEmpty()) {
            logger.info("Processando fotos do payload...");
            // Dica: No fluxo do Controller, evite chamar este método passando o objeto
            // que acabou de ser salvo (vistoriaSalva), pois ele já contém as fotos persistidas.
            for (com.necsus.necsusspring.model.VistoriaFoto foto : vistoriaPayload.getFotos()) {
                // Verifica se a foto já tem ID (já existe no banco) para não readicionar
                if (foto.getId() == null) {
                    foto.setVistoria(existing);
                    existing.getFotos().add(foto);
                }
            }
        }

        return vistoriaRepository.save(existing);
    }

    @Transactional
    public Vistoria updateWithNewPhotos(Long id, String observacoes, List<com.necsus.necsusspring.model.VistoriaFoto> novasFotos) {
        logger.info("=== UPDATE VISTORIA WITH NEW PHOTOS ===");

        Vistoria existing = vistoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vistoria não encontrada com id " + id));

        if (observacoes != null) {
            existing.setObservacoes(observacoes);
        }

        if (novasFotos != null && !novasFotos.isEmpty()) {
            logger.info("Adicionando {} novas fotos", novasFotos.size());
            for (com.necsus.necsusspring.model.VistoriaFoto foto : novasFotos) {
                foto.setVistoria(existing);
                existing.getFotos().add(foto);
            }
        }

        return vistoriaRepository.save(existing);
    }

    // === MÉTODO NOVO: SALVA SÓ A ANÁLISE DA IA (SEM MEXER EM FOTOS) ===
    @Transactional
    public void updateAnaliseIa(Long id, String analise) {
        logger.info("Atualizando apenas Análise IA para Vistoria ID: {}", id);
        vistoriaRepository.findById(id).ifPresent(v -> {
            v.setAnaliseIa(analise);
            vistoriaRepository.save(v);
        });
    }
    // ==================================================================

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