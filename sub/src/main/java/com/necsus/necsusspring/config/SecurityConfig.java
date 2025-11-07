package com.necsus.necsusspring.config;

import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.service.UserAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ADMIN_ROLES = RoleType.adminRoleCodes();
    private static final String[] USER_MANAGEMENT_ROLES = RoleType.userManagementRoleCodes();
    private static final String[] AUTHENTICATED_ROLES = RoleType.allRoleCodes();

    private final UserAccountService userAccountService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    public SecurityConfig(UserAccountService userAccountService,
                          PasswordEncoder passwordEncoder,
                          AuthenticationSuccessHandler authenticationSuccessHandler) {
        this.userAccountService = userAccountService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/h2-console/**"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/images/**"),
                                new AntPathRequestMatcher("/login"),
                                new AntPathRequestMatcher("/register"),
                                new AntPathRequestMatcher("/logout"),
                                new AntPathRequestMatcher("/logout/success"),
                                new AntPathRequestMatcher("/error"),
                                new AntPathRequestMatcher("/favicon.ico")
                        ).permitAll()
                        .requestMatchers(
                                new AntPathRequestMatcher("/admin/users"),
                                new AntPathRequestMatcher("/admin/users/**")
                        ).hasAnyRole(USER_MANAGEMENT_ROLES)
                        .requestMatchers(
                                new AntPathRequestMatcher("/dashboard/**"),
                                new AntPathRequestMatcher("/partners/**"),
                                new AntPathRequestMatcher("/vehicles/**"),
                                new AntPathRequestMatcher("/events/**"),
                                new AntPathRequestMatcher("/pagamentos/**"),
                                new AntPathRequestMatcher("/reports/**"),
                                new AntPathRequestMatcher("/admin/**")
                        ).hasAnyRole(ADMIN_ROLES)
                        .requestMatchers(
                                new AntPathRequestMatcher("/me/**")
                        ).hasAnyRole(AUTHENTICATED_ROLES)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/perform-logout")
                        .logoutSuccessUrl("/logout/success")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userAccountService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}
