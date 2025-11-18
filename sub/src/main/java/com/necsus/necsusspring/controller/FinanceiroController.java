package com.necsus.necsusspring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/financeiro")
public class FinanceiroController {

    @GetMapping
    public String dashboard(Model model) {
        configurePage(model, "dashboard", "Visão geral", "Acompanhe os principais indicadores financeiros do sistema.");
        return "financeiro/dashboard";
    }

    @GetMapping("/lancamentos")
    public String lancamentos(Model model) {
        configurePage(model, "lancamentos", "Lançamentos", "Registre entradas, saídas e mantenha o fluxo diário organizado.");
        return "financeiro/lancamentos";
    }

    @GetMapping("/movimento-caixa")
    public String movimentoCaixa(Model model) {
        configurePage(model, "movimento-caixa", "Movimento de Caixa", "Visualize a evolução do caixa e acompanhe tendências.");
        return "financeiro/movimento-caixa";
    }

    @GetMapping("/cadastros")
    public String cadastros(Model model) {
        configurePage(model, "cadastros", "Cadastros", "Gerencie contas, categorias e configurações financeiras.");
        return "financeiro/cadastros";
    }

    @GetMapping("/ferramentas")
    public String ferramentas(Model model) {
        configurePage(model, "ferramentas", "Ferramentas", "Acesse utilitários e automações que aceleram seu dia a dia.");
        return "financeiro/ferramentas";
    }

    @GetMapping("/relatorios")
    public String relatorios(Model model) {
        configurePage(model, "relatorios", "Relatórios", "Centralize análises e exportações para apoiar decisões estratégicas.");
        return "financeiro/relatorios";
    }

    private void configurePage(Model model, String currentPage, String heroTitle, String heroSubtitle) {
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageTitle", "SUB - Financeiro | " + heroTitle);
        model.addAttribute("financeiroHeroTitle", heroTitle);
        model.addAttribute("financeiroHeroSubtitle", heroSubtitle);
    }
}
