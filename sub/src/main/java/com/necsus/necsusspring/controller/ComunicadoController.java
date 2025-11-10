package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Comunicado;
import com.necsus.necsusspring.service.ComunicadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/comunicados")
public class ComunicadoController {

    private static final Logger logger = LoggerFactory.getLogger(ComunicadoController.class);

    private final ComunicadoService comunicadoService;

    public ComunicadoController(ComunicadoService comunicadoService) {
        this.comunicadoService = comunicadoService;
    }

    @GetMapping
    public String index(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String userRole = getUserRole(authentication);

        // Bloqueia acesso de usuários admin - apenas usuários Associado (USER) podem acessar
        if (!"USER".equals(userRole)) {
            logger.warn("Acesso negado aos comunicados. Apenas role USER/Associado tem acesso. Role atual: {}", userRole);
            return "redirect:/";
        }

        // Busca apenas comunicados visíveis (ativos e não expirados)
        List<Comunicado> comunicados = comunicadoService.listVisiveis();

        model.addAttribute("pageTitle", "SUB - Comunicados");
        model.addAttribute("comunicados", comunicados);

        return "comunicados";
    }

    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                .findFirst()
                .orElse("USER");
    }
}
