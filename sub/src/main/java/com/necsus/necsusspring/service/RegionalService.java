package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.RegionalRequest;
import com.necsus.necsusspring.dto.RegionalResponse;
import com.necsus.necsusspring.model.Regional;
import com.necsus.necsusspring.repository.RegionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RegionalService {

    private final RegionalRepository regionalRepository;

    public RegionalService(RegionalRepository regionalRepository) {
        this.regionalRepository = regionalRepository;
    }

    /**
     * Lista todas as regionais
     */
    public List<RegionalResponse> findAll() {
        return regionalRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista regionais ativas
     */
    public List<RegionalResponse> findActive() {
        return regionalRepository.findByActive(true)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista regionais bloqueadas
     */
    public List<RegionalResponse> findInactive() {
        return regionalRepository.findByActive(false)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Busca regional por ID
     */
    public Optional<RegionalResponse> findById(Long id) {
        return regionalRepository.findById(id)
                .map(this::toResponse);
    }

    /**
     * Busca regional por código
     */
    public Optional<RegionalResponse> findByCode(String code) {
        return regionalRepository.findByCode(code)
                .map(this::toResponse);
    }

    /**
     * Cria uma nova regional
     */
    public RegionalResponse create(RegionalRequest request) {
        // Validações
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (request.code() == null || request.code().trim().isEmpty()) {
            throw new IllegalArgumentException("Código é obrigatório.");
        }

        String normalizedCode = normalizeCode(request.code());

        if (regionalRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Já existe uma regional com este código.");
        }
        if (regionalRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Já existe uma regional com este nome.");
        }

        Regional regional = new Regional();
        regional.setName(request.name());
        regional.setCode(normalizedCode);
        regional.setDescription(request.description());
        regional.setActive(true);

        Regional saved = regionalRepository.save(regional);
        return toResponse(saved);
    }

    /**
     * Atualiza uma regional existente
     */
    public RegionalResponse update(Long id, RegionalRequest request) {
        Regional regional = regionalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Regional não encontrada: " + id));

        // Validações
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (request.code() == null || request.code().trim().isEmpty()) {
            throw new IllegalArgumentException("Código é obrigatório.");
        }

        String normalizedCode = normalizeCode(request.code());

        // Verifica se código já existe (exceto a própria regional)
        if (!regional.getCode().equals(normalizedCode) &&
            regionalRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Já existe uma regional com este código.");
        }

        // Verifica se nome já existe (exceto a própria regional)
        if (!regional.getName().equals(request.name()) &&
            regionalRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Já existe uma regional com este nome.");
        }

        regional.setName(request.name());
        regional.setCode(normalizedCode);
        regional.setDescription(request.description());

        Regional saved = regionalRepository.save(regional);
        return toResponse(saved);
    }

    /**
     * Ativa uma regional
     */
    public RegionalResponse activate(Long id) {
        Regional regional = regionalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Regional não encontrada: " + id));

        regional.setActive(true);
        Regional saved = regionalRepository.save(regional);
        return toResponse(saved);
    }

    /**
     * Desativa uma regional
     */
    public RegionalResponse deactivate(Long id) {
        Regional regional = regionalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Regional não encontrada: " + id));

        regional.setActive(false);
        Regional saved = regionalRepository.save(regional);
        return toResponse(saved);
    }

    /**
     * Conta regionais ativas
     */
    public Long countActive() {
        return regionalRepository.findByActive(true)
                .stream()
                .count();
    }

    /**
     * Conta regionais inativas
     */
    public Long countInactive() {
        return regionalRepository.findByActive(false)
                .stream()
                .count();
    }

    /**
     * Converte Regional para RegionalResponse
     */
    private RegionalResponse toResponse(Regional regional) {
        return new RegionalResponse(
                regional.getId(),
                regional.getName(),
                regional.getCode(),
                regional.getDescription(),
                regional.getActive(),
                regional.getCreatedAt()
        );
    }

    /**
     * Normaliza o código (uppercase e remove espaços)
     */
    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toUpperCase().replace(" ", "_");
    }
}
