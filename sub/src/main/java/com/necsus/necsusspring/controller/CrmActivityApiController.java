package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.CrmActivityResponse;
import com.necsus.necsusspring.dto.CreateCrmActivityRequest;
import com.necsus.necsusspring.service.CrmActivityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm/atividades")
@PreAuthorize("hasAnyRole('ADMIN', 'COMERCIAL', 'CLOSERS', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class CrmActivityApiController {

    private final CrmActivityService crmActivityService;

    public CrmActivityApiController(CrmActivityService crmActivityService) {
        this.crmActivityService = crmActivityService;
    }

    @GetMapping
    public List<CrmActivityResponse> list() {
        return crmActivityService.listAll();
    }

    @PostMapping
    public ResponseEntity<CrmActivityResponse> create(@Valid @RequestBody CreateCrmActivityRequest request) {
        CrmActivityResponse response = crmActivityService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
