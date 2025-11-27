package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.CompanyUserRequest;
import com.necsus.necsusspring.dto.CompanyUserResponse;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.UserAccountRepository;
import com.necsus.necsusspring.service.CompanyUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crm/api/usuarios")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class CompanyUserController {

    private final CompanyUserService companyUserService;
    private final UserAccountRepository userAccountRepository;

    public CompanyUserController(CompanyUserService companyUserService, UserAccountRepository userAccountRepository) {
        this.companyUserService = companyUserService;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Lista todos os consultores
     */
    @GetMapping
    public ResponseEntity<List<CompanyUserResponse>> getAllConsultants() {
        List<CompanyUserResponse> consultants = companyUserService.findAllConsultants();
        return ResponseEntity.ok(consultants);
    }

    /**
     * Busca consultor por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CompanyUserResponse> getConsultantById(@PathVariable Long id) {
        return companyUserService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lista consultores ativos
     */
    @GetMapping("/ativos")
    public ResponseEntity<List<CompanyUserResponse>> getActiveConsultants() {
        List<CompanyUserResponse> consultants = companyUserService.findActiveConsultants();
        return ResponseEntity.ok(consultants);
    }

    /**
     * Lista consultores bloqueados
     */
    @GetMapping("/bloqueados")
    public ResponseEntity<List<CompanyUserResponse>> getBlockedConsultants() {
        List<CompanyUserResponse> consultants = companyUserService.findBlockedConsultants();
        return ResponseEntity.ok(consultants);
    }

    /**
     * Cria um novo consultor
     */
    @PostMapping
    public ResponseEntity<?> createConsultant(@Valid @RequestBody CompanyUserRequest request) {
        try {
            CompanyUserResponse created = companyUserService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Atualiza um consultor
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateConsultant(@PathVariable Long id, @Valid @RequestBody CompanyUserRequest request) {
        try {
            CompanyUserResponse updated = companyUserService.update(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Bloqueia um consultor
     */
    @PutMapping("/{id}/bloquear")
    public ResponseEntity<?> blockConsultant(@PathVariable Long id) {
        try {
            CompanyUserResponse blocked = companyUserService.block(id);
            return ResponseEntity.ok(blocked);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Desbloqueia um consultor
     */
    @PutMapping("/{id}/desbloquear")
    public ResponseEntity<?> unblockConsultant(@PathVariable Long id) {
        try {
            CompanyUserResponse unblocked = companyUserService.unblock(id);
            return ResponseEntity.ok(unblocked);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retorna estatísticas de consultores
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Long activeCount = companyUserService.countActive();
        Long blockedCount = companyUserService.countBlocked();

        return ResponseEntity.ok(Map.of(
                "ativos", activeCount,
                "bloqueados", blockedCount
        ));
    }

    /**
     * Retorna o saldo do usuário logado
     */
    @GetMapping("/meu-saldo")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMERCIAL', 'CLOSERS', 'DIRETORIA', 'GERENTE', 'GESTOR')")
    public ResponseEntity<Map<String, Object>> getMeuSaldo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            UserAccount user = userAccountRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            return ResponseEntity.ok(Map.of(
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "fullName", user.getFullName(),
                    "saldo", user.getSaldo()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retorna o saldo de um usuário específico (apenas ADMIN)
     */
    @GetMapping("/{id}/saldo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSaldoUsuario(@PathVariable Long id) {
        try {
            UserAccount user = userAccountRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            return ResponseEntity.ok(Map.of(
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "fullName", user.getFullName(),
                    "saldo", user.getSaldo()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
