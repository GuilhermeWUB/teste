package com.necsus.necsusspring.config;

import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    private final String defaultAdminFullName;
    private final String defaultAdminUsername;
    private final String defaultAdminEmail;
    private final String defaultAdminPassword;

    public AdminUserInitializer(UserAccountRepository userAccountRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.default-full-name:Administrador}") String defaultAdminFullName,
                                @Value("${app.admin.default-username:admin}") String defaultAdminUsername,
                                @Value("${app.admin.default-email:admin@sub.com}") String defaultAdminEmail,
                                @Value("${app.admin.default-password:admin123}") String defaultAdminPassword) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultAdminFullName = defaultAdminFullName;
        this.defaultAdminUsername = defaultAdminUsername;
        this.defaultAdminEmail = defaultAdminEmail;
        this.defaultAdminPassword = defaultAdminPassword;
    }

    @Override
    public void run(String... args) {
        if (userAccountRepository.countByRole(RoleType.ADMIN.getCode()) > 0) {
            return;
        }

        UserAccount adminUser = new UserAccount();
        adminUser.setFullName(defaultAdminFullName);
        adminUser.setUsername(defaultAdminUsername);
        adminUser.setEmail(defaultAdminEmail);
        adminUser.setPassword(passwordEncoder.encode(defaultAdminPassword));
        adminUser.setRole(RoleType.ADMIN.getCode());

        userAccountRepository.save(adminUser);
        LOGGER.info("Usuário administrador padrão '{}' criado.", defaultAdminUsername);
    }
}
