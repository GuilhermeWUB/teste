package com.necsus.necsusspring.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller para gerenciar as páginas do módulo CRM
 * Acessível para equipes comerciais e gestão
 */
@Controller
@RequestMapping("/crm")
@PreAuthorize("hasAnyRole('ADMIN', 'COMERCIAL', 'CLOSERS', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class CrmController {

    /**
     * Página de Funil de Filiação (Kanban de negociações)
     */
    @GetMapping(value = "/vendas", produces = MediaType.TEXT_HTML_VALUE)
    public String vendas(Model model) {
        model.addAttribute("pageTitle", "Funil de Filiação - CRM");
        return "crm/vendas";
    }
}
