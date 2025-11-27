package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.CompanyUserRequest;
import com.necsus.necsusspring.dto.CompanyUserResponse;
import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompanyUserService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    // Role fixo para consultores
    private static final String CONSULTANT_ROLE = RoleType.COMERCIAL.getCode();

    public CompanyUserService(UserAccountRepository userAccountRepository,
                             PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lista todos os consultores (usuários com role COMERCIAL)
     */
    public List<CompanyUserResponse> findAllConsultants() {
        List<UserAccount> consultants = userAccountRepository.findByRoleIn(List.of(CONSULTANT_ROLE));
        return consultants.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista consultores ativos
     */
    public List<CompanyUserResponse> findActiveConsultants() {
        return userAccountRepository.findByRoleIn(List.of(CONSULTANT_ROLE))
                .stream()
                .filter(user -> user.getActive() != null && user.getActive())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista consultores bloqueados
     */
    public List<CompanyUserResponse> findBlockedConsultants() {
        return userAccountRepository.findByRoleIn(List.of(CONSULTANT_ROLE))
                .stream()
                .filter(user -> user.getActive() != null && !user.getActive())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Busca consultor por ID
     */
    public Optional<CompanyUserResponse> findById(Long id) {
        return userAccountRepository.findById(id)
                .filter(user -> CONSULTANT_ROLE.equals(user.getRole()))
                .map(this::toResponse);
    }

    /**
     * Cria um novo consultor
     */
    public CompanyUserResponse create(CompanyUserRequest request) {
        // Validações
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Já existe um usuário com este nome de usuário.");
        }
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe um usuário com este email.");
        }

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(CONSULTANT_ROLE); // Sempre COMERCIAL
        user.setActive(true); // Novo usuário sempre ativo

        UserAccount saved = userAccountRepository.save(user);
        return toResponse(saved);
    }

    /**
     * Atualiza um consultor existente
     */
    public CompanyUserResponse update(Long id, CompanyUserRequest request) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));

        // Verifica se é realmente um consultor
        if (!CONSULTANT_ROLE.equals(user.getRole())) {
            throw new IllegalArgumentException("Este usuário não é um consultor.");
        }

        // Verifica se username já existe (exceto o próprio usuário)
        if (!user.getUsername().equals(request.username()) &&
            userAccountRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Já existe um usuário com este nome de usuário.");
        }

        // Verifica se email já existe (exceto o próprio usuário)
        if (!user.getEmail().equals(request.email()) &&
            userAccountRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe um usuário com este email.");
        }

        user.setFullName(request.fullName());
        user.setUsername(request.username());
        user.setEmail(request.email());

        // Só atualiza a senha se foi fornecida
        if (request.password() != null && !request.password().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        UserAccount saved = userAccountRepository.save(user);
        return toResponse(saved);
    }

    /**
     * Bloqueia um consultor
     */
    public CompanyUserResponse block(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));

        if (!CONSULTANT_ROLE.equals(user.getRole())) {
            throw new IllegalArgumentException("Este usuário não é um consultor.");
        }

        user.setActive(false);
        UserAccount saved = userAccountRepository.save(user);
        return toResponse(saved);
    }

    /**
     * Desbloqueia um consultor
     */
    public CompanyUserResponse unblock(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));

        if (!CONSULTANT_ROLE.equals(user.getRole())) {
            throw new IllegalArgumentException("Este usuário não é um consultor.");
        }

        user.setActive(true);
        UserAccount saved = userAccountRepository.save(user);
        return toResponse(saved);
    }

    /**
     * Conta consultores ativos
     */
    public Long countActive() {
        return userAccountRepository.findByRoleIn(List.of(CONSULTANT_ROLE))
                .stream()
                .filter(user -> user.getActive() != null && user.getActive())
                .count();
    }

    /**
     * Conta consultores bloqueados
     */
    public Long countBlocked() {
        return userAccountRepository.findByRoleIn(List.of(CONSULTANT_ROLE))
                .stream()
                .filter(user -> user.getActive() != null && !user.getActive())
                .count();
    }

    /**
     * Converte UserAccount para CompanyUserResponse
     */
    private CompanyUserResponse toResponse(UserAccount user) {
        return new CompanyUserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                RoleType.displayNameFor(user.getRole()),
                user.getCreatedAt(),
                user.getActive()
        );
    }
}
