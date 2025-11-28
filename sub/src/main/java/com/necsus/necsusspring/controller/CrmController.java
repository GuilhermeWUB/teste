package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Sale;
import com.necsus.necsusspring.service.SaleService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * Controller para gerenciar as páginas do módulo CRM
 * Acessível para equipes comerciais e gestão
 */
@Controller
@RequestMapping("/crm")
@PreAuthorize("hasAnyRole('ADMIN', 'COMERCIAL', 'CLOSERS', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class CrmController {

    private final SaleService saleService;

    public CrmController(SaleService saleService) {
        this.saleService = saleService;
    }

    /**
     * Dashboard principal do módulo CRM
     * Exibe métricas gerais e acesso rápido às funcionalidades
     */
    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard - CRM");
        return "crm/dashboard";
    }

    /**
     * Página de Funil de Filiação (Kanban de negociações)
     */
    @GetMapping(value = "/vendas", produces = MediaType.TEXT_HTML_VALUE)
    public String vendas(Model model) {
        model.addAttribute("pageTitle", "Funil de Filiação - CRM");
        return "crm/vendas";
    }

    /**
     * Página de visualização/edição de uma venda específica
     * Retorna apenas o conteúdo HTML para ser carregado via AJAX
     */
    @GetMapping(value = "/vendas/{id}/view", produces = MediaType.TEXT_HTML_VALUE)
    public String viewVenda(@PathVariable Long id, Model model) {
        Optional<Sale> saleOpt = saleService.findById(id);

        if (saleOpt.isEmpty()) {
            model.addAttribute("error", "Venda não encontrada");
            return "crm/venda-view";
        }

        Sale sale = saleOpt.get();
        model.addAttribute("vendaId", id);
        model.addAttribute("venda", sale);
        model.addAttribute("valorVenda", sale.getValorVenda() != null ? sale.getValorVenda() : 0.0);

        return "crm/venda-view";
    }

    @GetMapping(value = "/contatos", produces = MediaType.TEXT_HTML_VALUE)
    public String contatos(Model model) {
        model.addAttribute("pageTitle", "Contatos - CRM");
        return "crm/contatos";
    }

    @GetMapping(value = "/atividades", produces = MediaType.TEXT_HTML_VALUE)
    public String atividades(Model model) {
        model.addAttribute("pageTitle", "Atividades - CRM");
        return "crm/atividades";
    }

    @GetMapping(value = "/minha-conta", produces = MediaType.TEXT_HTML_VALUE)
    public String minhaConta(Model model) {
        return buildComingSoonPage(model, "Minha Conta");
    }

    @GetMapping(value = "/relatorios", produces = MediaType.TEXT_HTML_VALUE)
    public String relatorios(Model model) {
        model.addAttribute("pageTitle", "Relatórios - CRM");
        return "crm/relatorios";
    }

    @GetMapping(value = "/financeiro", produces = MediaType.TEXT_HTML_VALUE)
    public String financeiro(Model model) {
        return buildComingSoonPage(model, "Financeiro");
    }

    @GetMapping(value = "/minha-empresa", produces = MediaType.TEXT_HTML_VALUE)
    public String minhaEmpresa(Model model) {
        return "redirect:/crm/minha-empresa/usuarios";
    }

    @GetMapping(value = "/minha-empresa/usuarios", produces = MediaType.TEXT_HTML_VALUE)
    public String minhaEmpresaUsuarios(Model model) {
        return buildMinhaEmpresaPage(model, "usuarios");
    }

    @GetMapping(value = "/minha-empresa/regionais", produces = MediaType.TEXT_HTML_VALUE)
    public String minhaEmpresaRegionais(Model model) {
        return buildMinhaEmpresaPage(model, "regionais");
    }

    @GetMapping(value = "/ferramentas", produces = MediaType.TEXT_HTML_VALUE)
    public String ferramentas(Model model) {
        return buildComingSoonPage(model, "Ferramentas");
    }

    private String buildComingSoonPage(Model model, String sectionTitle) {
        model.addAttribute("pageTitle", sectionTitle + " - CRM");
        model.addAttribute("sectionTitle", sectionTitle);
        model.addAttribute("sectionDescription", "Esta área do CRM está em desenvolvimento. Em breve teremos novidades.");
        return "crm/em-desenvolvimento";
    }

    private String buildMinhaEmpresaPage(Model model, String section) {
        model.addAttribute("pageTitle", "Minha Empresa - CRM");
        model.addAttribute("defaultSection", section);
        return "crm/minha-empresa";
    }
}
