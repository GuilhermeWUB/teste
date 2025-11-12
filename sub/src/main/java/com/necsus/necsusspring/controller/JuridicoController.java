package com.necsus.necsusspring.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller para gerenciar as páginas do módulo Jurídico
 * Acessível apenas para administradores
 */
@Controller
@RequestMapping("/juridico")
@PreAuthorize("hasRole('ADMIN')")
public class JuridicoController {

    /**
     * Página de Cobrança
     */
    @GetMapping(value = "/cobranca", produces = MediaType.TEXT_HTML_VALUE)
    public String cobranca(Model model) {
        model.addAttribute("pageTitle", "Cobrança - Jurídico");
        return "juridico/cobranca";
    }

    /**
     * Página de Cobrança Fidelidade
     */
    @GetMapping(value = "/cobranca-fidelidade", produces = MediaType.TEXT_HTML_VALUE)
    public String cobrancaFidelidade(Model model) {
        model.addAttribute("pageTitle", "Cobrança Fidelidade - Jurídico");
        return "juridico/cobranca_fidelidade";
    }

    /**
     * Página de Processos em Geral
     */
    @GetMapping(value = "/processos", produces = MediaType.TEXT_HTML_VALUE)
    public String processos(Model model) {
        model.addAttribute("pageTitle", "Processos em Geral - Jurídico");
        return "juridico/processos";
    }

    /**
     * Página de Acordos a pagar
     */
    @GetMapping(value = "/acordos", produces = MediaType.TEXT_HTML_VALUE)
    public String acordos(Model model) {
        model.addAttribute("pageTitle", "Acordos a pagar - Jurídico");
        return "juridico/acordos";
    }
}
