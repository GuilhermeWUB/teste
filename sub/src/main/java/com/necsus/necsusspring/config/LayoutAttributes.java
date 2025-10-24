package com.necsus.necsusspring.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LayoutAttributes {

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn(HttpServletRequest request) {
        // request nunca vem nulo em MVC; remoteUser será null se não autenticado
        return request != null && request.getRemoteUser() != null;
    }
}