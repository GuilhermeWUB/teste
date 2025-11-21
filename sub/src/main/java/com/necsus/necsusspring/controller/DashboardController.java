package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.CrmActivity;
import com.necsus.necsusspring.dto.CrmDeal;
import com.necsus.necsusspring.dto.CrmLead;
import com.necsus.necsusspring.dto.CrmMetric;
import com.necsus.necsusspring.dto.DashboardSummary;
import com.necsus.necsusspring.model.Demand;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.service.DashboardService;
import com.necsus.necsusspring.service.DemandService;
import com.necsus.necsusspring.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final DemandService demandService;
    private final UserAccountService userAccountService;

    private static final int NEXT_DEMANDS_LIMIT = 3;

    public DashboardController(DashboardService dashboardService,
                               DemandService demandService,
                               UserAccountService userAccountService) {
        this.dashboardService = dashboardService;
        this.demandService = demandService;
        this.userAccountService = userAccountService;
    }

    @GetMapping({"/dashboard"})
    public String dashboard(Model model, Authentication authentication) {
        DashboardSummary summary = dashboardService.loadSummary();

        model.addAttribute("pageTitle", "SUB - Dashboard");
        model.addAttribute("totalPartners", summary.totalPartners());
        model.addAttribute("activeVehicles", summary.activeVehicles());
        model.addAttribute("pendingInvoices", summary.pendingInvoices());
        model.addAttribute("collectionProgress", summary.collectionProgress());
        model.addAttribute("nextDemands", loadNextDemands(authentication));

        return "dashboard";
    }

    @GetMapping("/dashboard/crm")
    public String crmDashboard(Model model) {
        model.addAttribute("pageTitle", "SUB - CRM");
        model.addAttribute("crmHeroTitle", "CRM de Vendas");
        model.addAttribute("crmHeroSubtitle", "Acompanhe o funil, tarefas e oportunidades sem sair do SUB.");
        model.addAttribute("crmMetrics", loadCrmMetrics());
        model.addAttribute("crmPipeline", loadCrmPipeline());
        model.addAttribute("crmActivities", loadCrmActivities());
        model.addAttribute("crmLeads", loadCrmLeads());

        return "dashboard-crm";
    }

    private List<Demand> loadNextDemands(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }

        Optional<UserAccount> currentUser = userAccountService.findByUsername(username);
        return currentUser
                .map(user -> demandService.findNextDemandsForUser(user, NEXT_DEMANDS_LIMIT))
                .orElse(Collections.emptyList());
    }

    private List<CrmMetric> loadCrmMetrics() {
        return List.of(
                new CrmMetric("Novos leads", "37", "+12% em relação à última semana", "Ativos", "bi-person-plus", "text-success"),
                new CrmMetric("Oportunidades", "14", "3 aguardando retorno", "Funil", "bi-kanban", "text-primary"),
                new CrmMetric("Taxa de conversão", "28%", "Subiu 5 pontos nesta semana", "Performance", "bi-graph-up-arrow", "text-warning"),
                new CrmMetric("Receita prevista", "R$ 142.500", "Considerando propostas enviadas", "Forecast", "bi-cash-stack", "text-info")
        );
    }

    private List<CrmDeal> loadCrmPipeline() {
        return List.of(
                new CrmDeal("Sistema de rastreamento", "AutoPrime", "R$ 18.500", "Proposta enviada", "20/07", "Quente"),
                new CrmDeal("Upgrade de planos", "LogiTrans", "R$ 24.900", "Negociação", "17/07", "Em tratativa"),
                new CrmDeal("Renovação anual", "Frota Sul", "R$ 36.200", "Fechamento", "15/07", "Confirmação"),
                new CrmDeal("Cobertura premium", "Veloz Delivery", "R$ 9.800", "Diagnóstico", "22/07", "Descoberta"),
                new CrmDeal("Pacote corporativo", "RotaMax", "R$ 53.100", "Prospecção", "30/07", "Em validação")
        );
    }

    private List<CrmActivity> loadCrmActivities() {
        return List.of(
                new CrmActivity("Follow-up com AutoPrime", "Rafaela Souza", "Hoje, 14:30", "Ligação"),
                new CrmActivity("Enviar proposta revisada para LogiTrans", "Caio Duarte", "Hoje, 16:00", "E-mail"),
                new CrmActivity("Preparar demo para Veloz Delivery", "Equipe Comercial", "Amanhã, 09:00", "Reunião"),
                new CrmActivity("Check-in com RotaMax", "Ana Lima", "Amanhã, 15:00", "Ligação")
        );
    }

    private List<CrmLead> loadCrmLeads() {
        return List.of(
                new CrmLead("Lucas Pereira", "Grupo Horizon", "Interesse em monitoramento", "Indicação"),
                new CrmLead("Marina Costa", "TechFleet", "Quer proposta corporativa", "Inbound"),
                new CrmLead("Diego Martins", "Expressa", "Comparando concorrentes", "Evento"),
                new CrmLead("Juliana Faria", "Log Brasil", "Upgrade de cobertura", "Base ativa")
        );
    }
}
