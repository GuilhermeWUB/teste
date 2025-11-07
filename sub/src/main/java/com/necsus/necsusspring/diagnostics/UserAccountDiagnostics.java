package com.necsus.necsusspring.diagnostics;

import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Componente de diagnóstico para identificar problemas com UserAccount
 * Este componente executa ANTES de qualquer outro para diagnosticar problemas
 */
@Component
@Order(1)  // Executa primeiro
public class UserAccountDiagnostics implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountDiagnostics.class);

    private final UserAccountRepository userAccountRepository;

    public UserAccountDiagnostics(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public void run(String... args) {
        LOGGER.info("========================================");
        LOGGER.info("DIAGNÓSTICO DE USERACCOUNT - INICIANDO");
        LOGGER.info("========================================");

        try {
            // Tenta contar usuários
            long count = userAccountRepository.count();
            LOGGER.info("Total de usuários no banco: {}", count);

            if (count == 0) {
                LOGGER.warn("ATENÇÃO: Nenhum usuário encontrado no banco de dados!");
                return;
            }

            // Tenta buscar todos os usuários
            LOGGER.info("Tentando buscar todos os usuários...");
            List<UserAccount> users = userAccountRepository.findAll();
            LOGGER.info("✓ Sucesso ao buscar {} usuários", users.size());

            // Verifica cada usuário
            for (int i = 0; i < users.size(); i++) {
                UserAccount user = users.get(i);
                LOGGER.info("--- Usuário {} ---", i + 1);

                try {
                    LOGGER.info("  ID: {}", user.getId());
                    LOGGER.info("  Username: {}", user.getUsername());
                    LOGGER.info("  Email: {}", user.getEmail());
                    LOGGER.info("  FullName: {}", user.getFullName());
                    LOGGER.info("  Role: {}", user.getRole());

                    // VERIFICAÇÃO CRÍTICA: created_at
                    LocalDateTime createdAt = user.getCreatedAt();
                    if (createdAt == null) {
                        LOGGER.error("  ⚠️ PROBLEMA ENCONTRADO: created_at é NULL!");
                        LOGGER.error("  ⚠️ Isso causará erro de TimeStamp ao renderizar a página!");

                        // Tenta corrigir automaticamente
                        LOGGER.info("  Tentando corrigir automaticamente...");
                        user.setCreatedAt(LocalDateTime.now());
                        userAccountRepository.save(user);
                        LOGGER.info("  ✓ created_at corrigido para: {}", user.getCreatedAt());
                    } else {
                        LOGGER.info("  CreatedAt: {} ✓", createdAt);
                    }

                } catch (Exception e) {
                    LOGGER.error("  ✗ ERRO ao acessar dados do usuário: {}", e.getMessage(), e);
                }
            }

            LOGGER.info("========================================");
            LOGGER.info("DIAGNÓSTICO CONCLUÍDO");
            LOGGER.info("========================================");

        } catch (Exception e) {
            LOGGER.error("========================================");
            LOGGER.error("ERRO CRÍTICO NO DIAGNÓSTICO!");
            LOGGER.error("Tipo: {}", e.getClass().getName());
            LOGGER.error("Mensagem: {}", e.getMessage());
            LOGGER.error("========================================", e);
        }
    }
}
