package com.necsus.necsusspring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    // Abre o dashboard em "/" e em "/dashboard"
    @GetMapping({"/dashboard"})
    public String dashboard(Model model) {

        // Título da página
        model.addAttribute("pageTitle", "SUB - Dashboard");

        // ===== Dados exibidos nos cards =====
        // Troque pelos valores do seu Service/Repository quando quiser.
        model.addAttribute("totalPartners", 0);     // ex.: partnersService.countAtivos()
        model.addAttribute("activeVehicles", 0);    // ex.: veiculosService.countAtivos()
        model.addAttribute("pendingInvoices", 0);   // ex.: cobrancaService.countPendentes()

        // Progresso de arrecadação (0 a 100)
        int collectionProgress = 0;                 // ex.: arrecadacaoService.progressPercent()
        model.addAttribute("collectionProgress", collectionProgress);

        return "dashboard"; // templates/dashboard.html
    }
}
