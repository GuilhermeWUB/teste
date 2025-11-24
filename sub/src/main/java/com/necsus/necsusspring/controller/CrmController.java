package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.CrmKanbanColumn;
import com.necsus.necsusspring.model.Sale;
import com.necsus.necsusspring.model.SaleStatus;
import com.necsus.necsusspring.service.SaleService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Página Overview do CRM
     */
    @GetMapping(value = "/overview", produces = MediaType.TEXT_HTML_VALUE)
    public String overview(Model model) {
        model.addAttribute("pageTitle", "Overview - CRM");
        return "crm/overview";
    }

    /**
     * Página de Funil de Filiação (Kanban de negociações)
     */
    @GetMapping(value = "/vendas", produces = MediaType.TEXT_HTML_VALUE)
    public String vendas(Model model) {
        List<Sale> sales = saleService.findAll();
        Map<SaleStatus, List<Sale>> salesByStatus = sales.stream()
            .collect(Collectors.groupingBy(Sale::getStatus));

        List<CrmKanbanColumn> columns = Stream.of(SaleStatus.values())
            .map(status -> new CrmKanbanColumn(
                status.getDisplayName(),
                "",
                String.valueOf(salesByStatus.getOrDefault(status, List.of()).size()),
                helperForStatus(status),
                salesByStatus.getOrDefault(status, List.of()).stream()
                    .map(this::mapToCard)
                    .toList()
            ))
            .toList();

        model.addAttribute("pageTitle", "Funil de Filiação - CRM");
        model.addAttribute("crmKanbanTitle", "Funil de Filiação");
        model.addAttribute("crmKanbanSubtitle", "Acompanhe cada etapa da venda em um fluxo estilo kanban.");
        model.addAttribute("crmKanbanColumns", columns);
        model.addAttribute("currentCrmPage", "vendas");
        model.addAttribute("crmFullWidth", true);

        return "crm-vendas";
    }

    private String helperForStatus(SaleStatus status) {
        return switch (status) {
            case COTACOES_RECEBIDAS -> "Novas solicitações aguardando retorno";
            case EM_NEGOCIACAO -> "Leads ativos em negociação";
            case VISTORIAS -> "Clientes com vistorias em andamento";
            case LIBERADAS_PARA_CADASTRO -> "Prontas para cadastro no sistema";
            case FILIACAO_CONCRETIZADAS -> "Negociações concluídas com sucesso";
        };
    }

    private CrmKanbanColumn.CrmKanbanDeal mapToCard(Sale sale) {
        String title = firstNonBlank(sale.getModelo(), sale.getTipoVeiculo(), "Negociação em andamento");
        String company = firstNonBlank(sale.getCooperativa(), sale.getPlaca(), "-");
        String status = firstNonBlank(sale.getOrigemLead(), sale.getEstado(), "Novo lead");
        String amount = firstNonBlank(sale.getCidade(), "--");
        String dueDate = sale.getCreatedAt() != null
            ? sale.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "--";

        return new CrmKanbanColumn.CrmKanbanDeal(
            sale.getStatus().getDisplayName(),
            title,
            company,
            status,
            amount,
            dueDate
        );
    }

    private String firstNonBlank(String... values) {
        return Stream.of(values)
            .filter(Predicate.not(String::isBlank))
            .findFirst()
            .orElse("");
    }
}
