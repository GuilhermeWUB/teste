package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller de teste/diagnóstico para verificar problemas com UserAccount
 * TEMPORÁRIO - Remover após resolver o problema
 */
@RestController
@RequestMapping("/api/diagnostics")
public class UserAccountDiagnosticController {

    private final UserAccountRepository userAccountRepository;

    public UserAccountDiagnosticController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<?> checkUsers() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Tenta contar
            long count = userAccountRepository.count();
            response.put("totalUsers", count);
            response.put("status", "OK");

            // Tenta buscar todos
            List<UserAccount> users = userAccountRepository.findAll();
            response.put("usersLoaded", users.size());

            // Verifica cada usuário
            for (int i = 0; i < users.size(); i++) {
                UserAccount user = users.get(i);
                Map<String, Object> userInfo = new HashMap<>();

                try {
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("fullName", user.getFullName());
                    userInfo.put("role", user.getRole());

                    // VERIFICAÇÃO CRÍTICA
                    try {
                        userInfo.put("createdAt", user.getCreatedAt());
                        userInfo.put("createdAtNull", user.getCreatedAt() == null);
                    } catch (Exception e) {
                        userInfo.put("createdAtError", e.getMessage());
                    }

                    response.put("user_" + i, userInfo);

                } catch (Exception e) {
                    userInfo.put("error", e.getMessage());
                    userInfo.put("errorType", e.getClass().getName());
                    response.put("user_" + i + "_ERROR", userInfo);
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getName());
            response.put("stackTrace", e.getStackTrace()[0].toString());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Diagnostic controller is working!");
    }
}
