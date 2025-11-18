package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.service.JinjavaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/financeiro")
public class FinanceiroController {

    private final JinjavaService jinjavaService;

    public FinanceiroController(JinjavaService jinjavaService) {
        this.jinjavaService = jinjavaService;
    }

    @GetMapping
    public String dashboard(Model model) {
        configurePage(model, "dashboard", "Visão geral", "Acompanhe os principais indicadores financeiros do sistema.");

        // Dados de exemplo para o gráfico de pizza (substituir por dados reais do banco)
        double totalEntradas = 15750.50;
        double totalSaidas = 8320.75;

        // Preparar contexto para o template Jinjava
        Map<String, Object> chartContext = new HashMap<>();
        chartContext.put("chartId", "chartEntradaSaida");
        chartContext.put("titulo", "Entradas vs Saidas - Mes Atual");
        chartContext.put("entradas", totalEntradas);
        chartContext.put("saidas", totalSaidas);

        // Renderizar o gráfico usando Jinjava
        String chartHtml = jinjavaService.render("chart-pizza-entrada-saida.html", chartContext);

        // Passar dados para o template Thymeleaf
        model.addAttribute("chartEntradaSaida", chartHtml);
        model.addAttribute("totalEntradas", totalEntradas);
        model.addAttribute("totalSaidas", totalSaidas);
        model.addAttribute("saldo", totalEntradas - totalSaidas);

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
