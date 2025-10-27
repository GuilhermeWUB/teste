package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.UserRegistrationDto;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Sort;

@Service
public class UserAccountService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    private static final Set<String> ALLOWED_ROLES = Set.of(ROLE_ADMIN, ROLE_USER);

    public UserAccountService(UserAccountRepository userAccountRepository,
                              PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount register(UserRegistrationDto registrationDto) {
        UserAccount userAccount = new UserAccount();
        userAccount.setFullName(registrationDto.getFullName());
        userAccount.setEmail(registrationDto.getEmail());
        userAccount.setUsername(registrationDto.getUsername());
        userAccount.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        userAccount.setRole(ROLE_USER);
        return userAccountRepository.save(userAccount);
    }

    public boolean existsByUsername(String username) {
        return username != null && userAccountRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return email != null && userAccountRepository.existsByEmail(email);
    }

    public List<UserAccount> findAll() {
        return userAccountRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName"));
    }

    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return userAccountRepository.findByUsername(username);
    }

    public Optional<UserAccount> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return userAccountRepository.findByEmail(email);
    }

    public void updateRole(Long id, String role) {
        String normalizedRole = normalizeRole(role);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Tipo de permissão inválido: " + role);
        }

        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (ROLE_ADMIN.equals(user.getRole()) && ROLE_USER.equals(normalizedRole)
                && userAccountRepository.countByRole(ROLE_ADMIN) <= 1) {
            throw new IllegalStateException("Não é possível remover o último administrador do sistema.");
        }

        user.setRole(normalizedRole);
        userAccountRepository.save(user);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return ROLE_USER;
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return User.withUsername(userAccount.getUsername())
                .password(userAccount.getPassword())
                .roles(userAccount.getRole())
                .build();
    }
}
