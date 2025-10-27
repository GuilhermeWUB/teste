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

@Service
public class UserAccountService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

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
        userAccount.setRole("ADMIN");
        return userAccountRepository.save(userAccount);
    }

    public boolean existsByUsername(String username) {
        return username != null && userAccountRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return email != null && userAccountRepository.existsByEmail(email);
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
