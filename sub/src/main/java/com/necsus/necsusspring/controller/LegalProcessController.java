package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.LegalProcessRequest;
import com.necsus.necsusspring.model.LegalProcess;
import com.necsus.necsusspring.service.LegalProcessService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/juridico/api/processos", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public class LegalProcessController {

    private final LegalProcessService legalProcessService;

    public LegalProcessController(LegalProcessService legalProcessService) {
        this.legalProcessService = legalProcessService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LegalProcess> listProcesses() {
        return legalProcessService.findAll();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public LegalProcess getProcess(@PathVariable Long id) {
        return legalProcessService.findById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalProcess> createProcess(@Valid @RequestBody LegalProcessRequest request) {
        LegalProcess created = legalProcessService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public LegalProcess updateProcess(@PathVariable Long id, @Valid @RequestBody LegalProcessRequest request) {
        return legalProcessService.update(id, request);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteProcess(@PathVariable Long id) {
        legalProcessService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header(HttpHeaders.WARNING, ex.getMostSpecificCause().getMessage())
                .body(Map.of("message", "Número do processo já cadastrado."));
    }
}
