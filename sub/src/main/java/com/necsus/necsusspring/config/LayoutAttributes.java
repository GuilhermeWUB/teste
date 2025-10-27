package com.necsus.necsusspring.config;

import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Component
public class LayoutAttributes {

    private final UserAccountService userAccountService;

    public LayoutAttributes(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn(HttpServletRequest request) {
        // request nunca vem nulo em MVC; remoteUser será null se não autenticado
        return request != null && request.getRemoteUser() != null;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(RoleType::isAdminAuthority);
    }

    @ModelAttribute("currentUserFullName")
    public String currentUserFullName(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return null;
        }
        return userAccountService.findByUsername(authentication.getName())
                .map(UserAccount::getFullName)
                .orElse(authentication.getName());
    }

    @ModelAttribute("currentUsername")
    public String currentUsername(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return null;
        }
        return authentication.getName();
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName());
    }
}
