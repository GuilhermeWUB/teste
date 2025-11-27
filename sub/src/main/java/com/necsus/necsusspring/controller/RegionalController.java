package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.RegionalRequest;
import com.necsus.necsusspring.dto.RegionalResponse;
import com.necsus.necsusspring.service.RegionalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crm/api/regionais")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class RegionalController {

    private final RegionalService regionalService;

    public RegionalController(RegionalService regionalService) {
        this.regionalService = regionalService;
    }

    /**
     * Lista todas as regionais
     */
    @GetMapping
    public ResponseEntity<List<RegionalResponse>> getAllRegionais() {
        List<RegionalResponse> regionais = regionalService.findAll();
        return ResponseEntity.ok(regionais);
    }

    /**
     * Busca regional por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RegionalResponse> getRegionalById(@PathVariable Long id) {
        return regionalService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca regional por código
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<RegionalResponse> getRegionalByCode(@PathVariable String code) {
        return regionalService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lista regionais ativas
     */
    @GetMapping("/ativas")
    public ResponseEntity<List<RegionalResponse>> getActiveRegionais() {
        List<RegionalResponse> regionais = regionalService.findActive();
        return ResponseEntity.ok(regionais);
    }

    /**
     * Lista regionais inativas
     */
    @GetMapping("/inativas")
    public ResponseEntity<List<RegionalResponse>> getInactiveRegionais() {
        List<RegionalResponse> regionais = regionalService.findInactive();
        return ResponseEntity.ok(regionais);
    }

    /**
     * Cria uma nova regional
     */
    @PostMapping
    public ResponseEntity<?> createRegional(@Valid @RequestBody RegionalRequest request) {
        try {
            RegionalResponse created = regionalService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Atualiza uma regional
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRegional(@PathVariable Long id, @Valid @RequestBody RegionalRequest request) {
        try {
            RegionalResponse updated = regionalService.update(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Ativa uma regional
     */
    @PutMapping("/{id}/ativar")
    public ResponseEntity<?> activateRegional(@PathVariable Long id) {
        try {
            RegionalResponse activated = regionalService.activate(id);
            return ResponseEntity.ok(activated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Desativa uma regional
     */
    @PutMapping("/{id}/desativar")
    public ResponseEntity<?> deactivateRegional(@PathVariable Long id) {
        try {
            RegionalResponse deactivated = regionalService.deactivate(id);
            return ResponseEntity.ok(deactivated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retorna estatísticas de regionais
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Long activeCount = regionalService.countActive();
        Long inactiveCount = regionalService.countInactive();

        return ResponseEntity.ok(Map.of(
                "ativas", activeCount,
                "inativas", inactiveCount
        ));
    }
}
