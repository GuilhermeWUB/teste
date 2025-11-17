package com.necsus.necsusspring.config;

import com.necsus.necsusspring.model.RoleType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class RoleBasedAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String ADMIN_TARGET_URL = "/dashboard";
    private static final String USER_TARGET_URL = "/me/plan";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(authentication.getAuthorities());
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String determineTargetUrl(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return USER_TARGET_URL;
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(RoleType::isAdminAuthority)
                ? ADMIN_TARGET_URL
                : USER_TARGET_URL;
    }
}
