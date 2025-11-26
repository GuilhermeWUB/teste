package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.DashboardMetrics;
import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.repository.CrmActivityRepository;
import com.necsus.necsusspring.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CrmDashboardService {

    private final SaleRepository saleRepository;
    private final CrmActivityRepository activityRepository;

    public CrmDashboardService(SaleRepository saleRepository, CrmActivityRepository activityRepository) {
        this.saleRepository = saleRepository;
        this.activityRepository = activityRepository;
    }

    public DashboardMetrics getDashboardMetrics() {
        // Métricas de Vendas
        Long totalVendas = saleRepository.count();
        Long vendasConcluidas = saleRepository.countByConcluida(true);
        Long vendasEmNegociacao = saleRepository.countByStatus(SaleStatus.EM_NEGOCIACAO);

        // Calcular receita total e do mês atual
        List<Sale> allSales = saleRepository.findAll();
        Double totalReceita = allSales.stream()
            .filter(sale -> sale.getValorVenda() != null && sale.getConcluida())
            .mapToDouble(Sale::getValorVenda)
            .sum();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        Double receitaMesAtual = allSales.stream()
            .filter(sale -> sale.getValorVenda() != null
                && sale.getConcluida()
                && sale.getDataConclusao() != null
                && !sale.getDataConclusao().isBefore(startOfMonth)
                && !sale.getDataConclusao().isAfter(endOfMonth))
            .mapToDouble(Sale::getValorVenda)
            .sum();

        // Vendas por status
        Map<String, Long> vendasPorStatus = new HashMap<>();
        for (SaleStatus status : SaleStatus.values()) {
            Long count = saleRepository.countByStatus(status);
            vendasPorStatus.put(status.getDisplayName(), count);
        }

        // Métricas de Atividades
        Long totalAtividades = activityRepository.count();
        Long atividadesAgendadas = activityRepository.countByStatus(ActivityStatus.AGENDADA);
        Long atividadesConcluidas = activityRepository.countByStatus(ActivityStatus.CONCLUIDA);

        // Atividades de hoje
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        Long atividadesHoje = activityRepository.findByDataAgendadaBetween(startOfDay, endOfDay).size();

        // Atividades por tipo
        Map<String, Long> atividadesPorTipo = new HashMap<>();
        for (ActivityType tipo : ActivityType.values()) {
            Long count = activityRepository.countByTipo(tipo);
            atividadesPorTipo.put(tipo.getDisplayName(), count);
        }

        // Atividades por status
        Map<String, Long> atividadesPorStatus = new HashMap<>();
        for (ActivityStatus status : ActivityStatus.values()) {
            Long count = activityRepository.countByStatus(status);
            atividadesPorStatus.put(status.getDisplayName(), count);
        }

        // Taxa de conversão
        Double taxaConversao = totalVendas > 0
            ? (vendasConcluidas.doubleValue() / totalVendas.doubleValue()) * 100
            : 0.0;

        return new DashboardMetrics(
            totalVendas,
            vendasConcluidas,
            vendasEmNegociacao,
            totalReceita,
            receitaMesAtual,
            vendasPorStatus,
            totalAtividades,
            atividadesAgendadas,
            atividadesConcluidas,
            Long.valueOf(atividadesHoje),
            atividadesPorTipo,
            atividadesPorStatus,
            taxaConversao
        );
    }

    public Map<String, Object> getSalesMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Long totalVendas = saleRepository.count();
        Long vendasConcluidas = saleRepository.countByConcluida(true);

        List<Sale> allSales = saleRepository.findAll();
        Double totalReceita = allSales.stream()
            .filter(sale -> sale.getValorVenda() != null && sale.getConcluida())
            .mapToDouble(Sale::getValorVenda)
            .sum();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        Double receitaMesAtual = allSales.stream()
            .filter(sale -> sale.getValorVenda() != null
                && sale.getConcluida()
                && sale.getDataConclusao() != null
                && !sale.getDataConclusao().isBefore(startOfMonth)
                && !sale.getDataConclusao().isAfter(endOfMonth))
            .mapToDouble(Sale::getValorVenda)
            .sum();

        Map<String, Long> vendasPorStatus = new HashMap<>();
        for (SaleStatus status : SaleStatus.values()) {
            vendasPorStatus.put(status.getDisplayName(), saleRepository.countByStatus(status));
        }

        metrics.put("totalVendas", totalVendas);
        metrics.put("vendasConcluidas", vendasConcluidas);
        metrics.put("totalReceita", totalReceita);
        metrics.put("receitaMesAtual", receitaMesAtual);
        metrics.put("vendasPorStatus", vendasPorStatus);
        metrics.put("taxaConversao", totalVendas > 0 ? (vendasConcluidas.doubleValue() / totalVendas.doubleValue()) * 100 : 0.0);

        return metrics;
    }

    public Map<String, Object> getActivitiesMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Long totalAtividades = activityRepository.count();
        Long atividadesAgendadas = activityRepository.countByStatus(ActivityStatus.AGENDADA);
        Long atividadesConcluidas = activityRepository.countByStatus(ActivityStatus.CONCLUIDA);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        Long atividadesHoje = (long) activityRepository.findByDataAgendadaBetween(startOfDay, endOfDay).size();

        Map<String, Long> atividadesPorTipo = new HashMap<>();
        for (ActivityType tipo : ActivityType.values()) {
            atividadesPorTipo.put(tipo.getDisplayName(), activityRepository.countByTipo(tipo));
        }

        Map<String, Long> atividadesPorStatus = new HashMap<>();
        for (ActivityStatus status : ActivityStatus.values()) {
            atividadesPorStatus.put(status.getDisplayName(), activityRepository.countByStatus(status));
        }

        metrics.put("totalAtividades", totalAtividades);
        metrics.put("atividadesAgendadas", atividadesAgendadas);
        metrics.put("atividadesConcluidas", atividadesConcluidas);
        metrics.put("atividadesHoje", atividadesHoje);
        metrics.put("atividadesPorTipo", atividadesPorTipo);
        metrics.put("atividadesPorStatus", atividadesPorStatus);

        return metrics;
    }
}
