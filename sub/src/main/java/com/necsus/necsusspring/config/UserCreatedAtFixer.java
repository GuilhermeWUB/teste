package com.necsus.necsusspring.config;

import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corrige valores NULL na coluna created_at da tabela app_users.
 * Este componente é executado na inicialização da aplicação e garante que
 * todos os usuários tenham uma data de criação válida.
 */
@Component
public class UserCreatedAtFixer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserCreatedAtFixer.class);

    private final UserAccountRepository userAccountRepository;

    public UserCreatedAtFixer(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public void run(String... args) {
        List<UserAccount> allUsers = userAccountRepository.findAll();
        int fixedCount = 0;

        for (UserAccount user : allUsers) {
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(LocalDateTime.now());
                userAccountRepository.save(user);
                fixedCount++;
                LOGGER.info("Corrigido created_at NULL para usuário: {}", user.getUsername());
            }
        }

        if (fixedCount > 0) {
            LOGGER.info("Total de {} usuário(s) com created_at NULL corrigido(s).", fixedCount);
        } else {
            LOGGER.debug("Nenhum usuário com created_at NULL encontrado.");
        }
    }
}
