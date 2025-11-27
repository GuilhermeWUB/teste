package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.model.Withdrawal;
import com.necsus.necsusspring.repository.UserAccountRepository;
import com.necsus.necsusspring.service.WithdrawalService;
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
@RequestMapping("/crm/api/saques")
@PreAuthorize("hasAnyRole('ADMIN', 'COMERCIAL', 'CLOSERS', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final UserAccountRepository userAccountRepository;

    public WithdrawalController(WithdrawalService withdrawalService, UserAccountRepository userAccountRepository) {
        this.withdrawalService = withdrawalService;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Lista todos os saques (apenas ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Withdrawal>> getAllWithdrawals() {
        List<Withdrawal> withdrawals = withdrawalService.findAll();
        return ResponseEntity.ok(withdrawals);
    }

    /**
     * Lista saques do usuário logado
     */
    @GetMapping("/meus-saques")
    public ResponseEntity<List<Withdrawal>> getMyWithdrawals() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));

        List<Withdrawal> withdrawals = withdrawalService.findByUserId(user.getId());
        return ResponseEntity.ok(withdrawals);
    }

    /**
     * Busca um saque por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Withdrawal> getWithdrawalById(@PathVariable Long id) {
        return withdrawalService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtém o saldo disponível para saque do usuário logado
     */
    @GetMapping("/saldo-disponivel")
    public ResponseEntity<Map<String, Object>> getAvailableBalance() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));

        BigDecimal availableBalance = withdrawalService.getAvailableBalance(user.getId());

        return ResponseEntity.ok(Map.of(
                "saldoTotal", user.getSaldo(),
                "saldoDisponivel", availableBalance,
                "saldoBloqueado", user.getSaldo().subtract(availableBalance)
        ));
    }

    /**
     * Cria uma nova solicitação de saque
     */
    @PostMapping
    public ResponseEntity<?> createWithdrawalRequest(@RequestBody Map<String, Object> body) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            UserAccount user = userAccountRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));

            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String pixKey = body.get("pixKey").toString();

            Withdrawal withdrawal = withdrawalService.createWithdrawalRequest(user.getId(), amount, pixKey);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Solicitação de saque criada com sucesso!",
                    "withdrawal", withdrawal
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Aprova um saque (apenas ADMIN/DIRETORIA)
     */
    @PutMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRETORIA')")
    public ResponseEntity<?> approveWithdrawal(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String observation = (body != null) ? body.get("observation") : null;
            Withdrawal withdrawal = withdrawalService.approveWithdrawal(id, observation);

            return ResponseEntity.ok(Map.of(
                    "message", "Saque aprovado com sucesso!",
                    "withdrawal", withdrawal
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Rejeita um saque (apenas ADMIN/DIRETORIA)
     */
    @PutMapping("/{id}/rejeitar")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRETORIA')")
    public ResponseEntity<?> rejectWithdrawal(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String observation = body.get("observation");
            Withdrawal withdrawal = withdrawalService.rejectWithdrawal(id, observation);

            return ResponseEntity.ok(Map.of(
                    "message", "Saque rejeitado!",
                    "withdrawal", withdrawal
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Conclui um saque aprovado (apenas ADMIN)
     */
    @PutMapping("/{id}/concluir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> completeWithdrawal(@PathVariable Long id) {
        try {
            Withdrawal withdrawal = withdrawalService.completeWithdrawal(id);

            return ResponseEntity.ok(Map.of(
                    "message", "Saque concluído! Valor debitado do saldo.",
                    "withdrawal", withdrawal
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lista saques por status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRETORIA')")
    public ResponseEntity<List<Withdrawal>> getWithdrawalsByStatus(@PathVariable String status) {
        List<Withdrawal> withdrawals = withdrawalService.findByStatus(status.toUpperCase());
        return ResponseEntity.ok(withdrawals);
    }

    /**
     * Endpoint de TESTE: Cria um saque de teste para o usuário logado
     */
    @PostMapping("/teste")
    public ResponseEntity<?> createTestWithdrawal(@RequestBody(required = false) Map<String, Object> body) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            UserAccount user = userAccountRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));

            // Valor padrão de teste se não for fornecido
            BigDecimal amount = (body != null && body.containsKey("amount"))
                    ? new BigDecimal(body.get("amount").toString())
                    : new BigDecimal("100.00");

            Withdrawal withdrawal = withdrawalService.createTestWithdrawal(user.getId(), amount);

            return ResponseEntity.ok(Map.of(
                    "message", "Saque de teste criado com sucesso!",
                    "withdrawal", withdrawal,
                    "saldoAtual", user.getSaldo()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
