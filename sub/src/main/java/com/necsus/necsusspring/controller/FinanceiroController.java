package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.service.JinjavaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        // Grafico completo de categorias financeiras
        List<Map<String, Object>> categorias = new ArrayList<>();

        // Categorias de Entradas
        Map<String, Object> vendas = new HashMap<>();
        vendas.put("nome", "Vendas");
        vendas.put("valor", 8500.00);
        vendas.put("cor", "rgba(40, 167, 69, 0.8)");
        vendas.put("borderCor", "rgba(40, 167, 69, 1)");
        categorias.add(vendas);

        Map<String, Object> servicos = new HashMap<>();
        servicos.put("nome", "Servicos");
        servicos.put("valor", 4250.50);
        servicos.put("cor", "rgba(32, 201, 151, 0.8)");
        servicos.put("borderCor", "rgba(32, 201, 151, 1)");
        categorias.add(servicos);

        Map<String, Object> outrasEntradas = new HashMap<>();
        outrasEntradas.put("nome", "Outras Entradas");
        outrasEntradas.put("valor", 3000.00);
        outrasEntradas.put("cor", "rgba(13, 202, 240, 0.8)");
        outrasEntradas.put("borderCor", "rgba(13, 202, 240, 1)");
        categorias.add(outrasEntradas);

        // Categorias de Saidas
        Map<String, Object> fornecedores = new HashMap<>();
        fornecedores.put("nome", "Fornecedores");
        fornecedores.put("valor", 3500.00);
        fornecedores.put("cor", "rgba(220, 53, 69, 0.8)");
        fornecedores.put("borderCor", "rgba(220, 53, 69, 1)");
        categorias.add(fornecedores);

        Map<String, Object> salarios = new HashMap<>();
        salarios.put("nome", "Salarios");
        salarios.put("valor", 2800.00);
        salarios.put("cor", "rgba(255, 193, 7, 0.8)");
        salarios.put("borderCor", "rgba(255, 193, 7, 1)");
        categorias.add(salarios);

        Map<String, Object> despesasFixas = new HashMap<>();
        despesasFixas.put("nome", "Despesas Fixas");
        despesasFixas.put("valor", 1200.75);
        despesasFixas.put("cor", "rgba(108, 117, 125, 0.8)");
        despesasFixas.put("borderCor", "rgba(108, 117, 125, 1)");
        categorias.add(despesasFixas);

        Map<String, Object> impostos = new HashMap<>();
        impostos.put("nome", "Impostos");
        impostos.put("valor", 820.00);
        impostos.put("cor", "rgba(111, 66, 193, 0.8)");
        impostos.put("borderCor", "rgba(111, 66, 193, 1)");
        categorias.add(impostos);

        // Contexto para o grafico de categorias
        Map<String, Object> categoriasContext = new HashMap<>();
        categoriasContext.put("chartId", "chartCategorias");
        categoriasContext.put("titulo", "Distribuicao por Categorias - Mes Atual");
        categoriasContext.put("categorias", categorias);

        String chartCategoriasHtml = jinjavaService.render("chart-pizza-categorias.html", categoriasContext);
        model.addAttribute("chartCategorias", chartCategoriasHtml);

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
