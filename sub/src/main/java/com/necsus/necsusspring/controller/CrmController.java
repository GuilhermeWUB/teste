package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.CrmActivity;
import com.necsus.necsusspring.dto.CrmDeal;
import com.necsus.necsusspring.dto.CrmLead;
import com.necsus.necsusspring.dto.CrmMetric;
import com.necsus.necsusspring.dto.CrmPipelineColumn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/crm")
public class CrmController {

    @GetMapping({"", "/overview"})
    public String overview(Model model) {
        populateCrmModel(model, "overview", null);
        return "dashboard-crm";
    }

    @GetMapping("/vendas")
    public String vendas(Model model) {
        populateCrmModel(model, "vendas", "pipeline");
        return "dashboard-crm";
    }

    @GetMapping("/contatos")
    public String contatos(Model model) {
        populateCrmModel(model, "contatos", "leads");
        return "dashboard-crm";
    }

    @GetMapping("/atividades")
    public String atividades(Model model) {
        populateCrmModel(model, "atividades", "followups");
        return "dashboard-crm";
    }

    @GetMapping("/minha-conta")
    public String minhaConta(Model model) {
        populateCrmModel(model, "minha-conta", null);
        return "dashboard-crm";
    }

    @GetMapping("/relatorios")
    public String relatorios(Model model) {
        populateCrmModel(model, "relatorios", null);
        return "dashboard-crm";
    }

    @GetMapping("/financas")
    public String financas(Model model) {
        populateCrmModel(model, "financas", null);
        return "dashboard-crm";
    }

    @GetMapping("/minha-empresa")
    public String minhaEmpresa(Model model) {
        populateCrmModel(model, "minha-empresa", null);
        return "dashboard-crm";
    }

    @GetMapping("/ferramentas")
    public String ferramentas(Model model) {
        populateCrmModel(model, "ferramentas", null);
        return "dashboard-crm";
    }

    @GetMapping("/funil")
    public String pipeline(Model model) {
        populateCrmModel(model, "pipeline", "pipeline");
        return "dashboard-crm";
    }

    @GetMapping("/followups")
    public String followups(Model model) {
        populateCrmModel(model, "followups", "followups");
        return "dashboard-crm";
    }

    @GetMapping("/leads")
    public String leads(Model model) {
        populateCrmModel(model, "leads", "leads");
        return "dashboard-crm";
    }

    private void populateCrmModel(Model model, String currentPage, String focusSectionId) {
        model.addAttribute("pageTitle", "SUB - CRM");
        model.addAttribute("currentCrmPage", currentPage);
        model.addAttribute("crmFocusSection", focusSectionId);
        model.addAttribute("crmHeroTitle", "CRM de Vendas");
        model.addAttribute("crmHeroSubtitle", "Acompanhe o funil, tarefas e oportunidades sem sair do SUB.");
        model.addAttribute("crmMetrics", loadCrmMetrics());
        model.addAttribute("crmPipelineColumns", loadCrmPipelineColumns());
        model.addAttribute("crmActivities", loadCrmActivities());
        model.addAttribute("crmLeads", loadCrmLeads());
    }

    private List<CrmMetric> loadCrmMetrics() {
        return List.of(
                new CrmMetric("Novos leads", "37", "+12% em relação à última semana", "Ativos", "bi-person-plus", "text-success"),
                new CrmMetric("Oportunidades", "14", "3 aguardando retorno", "Funil", "bi-kanban", "text-primary"),
                new CrmMetric("Taxa de conversão", "28%", "Subiu 5 pontos nesta semana", "Performance", "bi-graph-up-arrow", "text-warning"),
                new CrmMetric("Receita prevista", "R$ 142.500", "Considerando propostas enviadas", "Forecast", "bi-cash-stack", "text-info")
        );
    }

    private List<CrmPipelineColumn> loadCrmPipelineColumns() {
        List<CrmDeal> quotes = List.of(
                new CrmDeal("Cotação APP Maxi Brasil", "Lidertech - Curitiba", "R$ 120,00", "Retorno pendente", "Hoje", "Acompanhar"),
                new CrmDeal("Adesão Star Assistência", "Star Assistência 24H", "R$ 72,00", "Documentação", "Hoje", "Ver proposta"),
                new CrmDeal("Cotação APP Axa Restituição", "Valter Mendes - Curitiba", "R$ 110,00", "Checagem", "Amanhã", "Fup"),
                new CrmDeal("Rastreamento Light + APP", "Herick Anderson - SP", "R$ 149,00", "Retorno", "Amanhã", "Fup")
        );

        List<CrmDeal> negotiating = List.of(
                new CrmDeal("Cotação APP Maxi Brasil", "Alexandre - Maringá", "R$ 155,00", "Contraproposta", "Hoje", "Negociando"),
                new CrmDeal("APP Porte para Aplicativo", "Andreia Fimatti - PR", "R$ 198,00", "Ajustar cobertura", "Hoje", "Urgente"),
                new CrmDeal("Renovação APP Ferrari Brasil", "Clecio Serapião - GO", "R$ 210,00", "Revisão", "Amanhã", "Acompanhar"),
                new CrmDeal("APP Passageiros Fidelização", "Samuel Gutierrez - PR", "R$ 140,00", "Rever franquia", "Amanhã", "Fup")
        );

        List<CrmDeal> wins = List.of(
                new CrmDeal("Assistência 24H", "Mauro Oliveira - PR", "R$ 85,00", "Fechada", "Hoje", "Vitória"),
                new CrmDeal("Fidelização Auto + Assistência", "Alexandre Gomes - PR", "R$ 132,00", "Fechada", "Hoje", "Vitória"),
                new CrmDeal("APP Maxi Brasil", "Ribamar Silva - SP", "R$ 175,00", "Fechada", "Hoje", "Vitória"),
                new CrmDeal("APP Maxi Brasil", "Adriano Cardozo - RJ", "R$ 165,00", "Fechada", "Hoje", "Vitória"),
                new CrmDeal("Cobertura Premium APP", "Carlos - Itajubá", "R$ 188,00", "Fechada", "Hoje", "Vitória")
        );

        List<CrmDeal> readyForOnboarding = List.of(
                new CrmDeal("APP + Assistência", "Elze Rodrigues - PR", "R$ 150,00", "Dados enviados", "Hoje", "Cadastro"),
                new CrmDeal("APP Porte Fidelização", "Rafael Canuto", "R$ 119,00", "Dados enviados", "Hoje", "Cadastro"),
                new CrmDeal("APP Maxi Brasil", "Santiago Aparecido - SP", "R$ 180,00", "Docs pendentes", "Amanhã", "Checar"),
                new CrmDeal("APP Peregrino", "Josiel Júnior - PR", "R$ 132,00", "Dados enviados", "Amanhã", "Cadastro"),
                new CrmDeal("APP Maxi Brasil", "Laercio Araújo", "R$ 142,00", "Dados enviados", "Amanhã", "Cadastro")
        );

        List<CrmDeal> closed = List.of(
                new CrmDeal("APP Maxi Brasil", "Erumarbe Custódio - PR", "R$ 140,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Katia Cristina - SP", "R$ 129,90", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Graziele Cristino - SP", "R$ 142,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Odasio Junior - DF", "R$ 142,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Lucas Cardoso - RJ", "R$ 125,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Liliane - PR", "R$ 122,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "João Victor - PR", "R$ 148,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Erivaldo Silva - PE", "R$ 146,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Aldicley Nascimento - PR", "R$ 120,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Antonio Cesário - RN", "R$ 135,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Larissa Amorim - RJ", "R$ 149,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Douglas Rodrigues - SP", "R$ 155,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Roberto Santos - GO", "R$ 132,00", "Ativo", "Hoje", "Concluído"),
                new CrmDeal("APP Maxi Brasil", "Daniel - SP", "R$ 140,00", "Ativo", "Hoje", "Concluído")
        );

        return List.of(
                new CrmPipelineColumn("Cotações recebidas", "Novos leads aguardando retorno", String.valueOf(quotes.size()), "bg-primary-subtle text-primary", quotes),
                new CrmPipelineColumn("Em negociação", "Em tratativa com o time comercial", String.valueOf(negotiating.size()), "bg-warning-subtle text-warning", negotiating),
                new CrmPipelineColumn("Vitórias", "Fechados com sucesso", String.valueOf(wins.size()), "bg-success-subtle text-success", wins),
                new CrmPipelineColumn("Liberado para cadastro", "Documentação validada", String.valueOf(readyForOnboarding.size()), "bg-info-subtle text-info", readyForOnboarding),
                new CrmPipelineColumn("Filiações concretizadas", "Em processo de ativação", String.valueOf(closed.size()), "bg-secondary-subtle text-secondary", closed)
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
