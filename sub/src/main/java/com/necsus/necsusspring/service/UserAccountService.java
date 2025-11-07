package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.UserRegistrationDto;
import com.necsus.necsusspring.model.RoleType;
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
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;

@Service
public class UserAccountService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> ADMIN_ROLES = RoleType.adminRoleCodeSet();
    private static final Set<String> ALLOWED_ROLES = RoleType.assignableRoles().stream()
            .map(RoleType::getCode)
            .collect(Collectors.toUnmodifiableSet());

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
        userAccount.setRole(RoleType.USER.getCode());
        return userAccountRepository.save(userAccount);
    }

    /**
     * Cria um novo usuário com um role específico (usado pelo admin)
     */
    public UserAccount createUser(String fullName, String username, String email, String password, String role) {
        // Validações
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Já existe um usuário com este nome de usuário.");
        }
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("Já existe um usuário com este email.");
        }

        String normalizedRole = normalizeRole(role);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Tipo de permissão inválido: " + role);
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setFullName(fullName);
        userAccount.setUsername(username);
        userAccount.setEmail(email);
        userAccount.setPassword(passwordEncoder.encode(password));
        userAccount.setRole(normalizedRole);
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

        RoleType targetRole = RoleType.fromCode(normalizedRole)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de permissão inválido: " + role));

        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        RoleType currentRole = RoleType.fromCode(user.getRole()).orElse(RoleType.USER);
        boolean currentIsAdmin = currentRole.hasAdminPrivileges();
        boolean targetIsAdmin = targetRole.hasAdminPrivileges();

        if (currentIsAdmin && !targetIsAdmin) {
            long adminCount = userAccountRepository.countByRoleIn(ADMIN_ROLES);
            if (adminCount <= 1) {
                throw new IllegalStateException("Não é possível remover o último usuário com acesso administrativo do sistema.");
            }
        }

        user.setRole(targetRole.getCode());
        userAccountRepository.save(user);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return RoleType.USER.getCode();
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        RoleType roleType = RoleType.fromCode(userAccount.getRole()).orElse(RoleType.USER);

        return User.withUsername(userAccount.getUsername())
                .password(userAccount.getPassword())
                .roles(roleType.getCode())
                .build();
    }
}
