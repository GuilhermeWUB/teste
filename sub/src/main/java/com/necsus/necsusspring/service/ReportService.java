package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.ActivityType;
import com.necsus.necsusspring.repository.CrmActivityRepository;
import com.necsus.necsusspring.repository.SaleRepository;
import com.necsus.necsusspring.repository.WithdrawalRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    private final CrmActivityRepository activityRepository;
    private final SaleRepository saleRepository;
    private final WithdrawalRepository withdrawalRepository;

    public ReportService(CrmActivityRepository activityRepository,
                        SaleRepository saleRepository,
                        WithdrawalRepository withdrawalRepository) {
        this.activityRepository = activityRepository;
        this.saleRepository = saleRepository;
        this.withdrawalRepository = withdrawalRepository;
    }

    /**
     * Gera relatório geral com métricas de ligações, vendas e saques
     */
    public Map<String, Object> getGeneralReport() {
        Map<String, Object> report = new HashMap<>();

        // Métricas de Ligações
        Long totalLigacoes = activityRepository.countByTipo(ActivityType.LIGACAO);
        report.put("totalLigacoes", totalLigacoes);

        // Métricas de Vendas
        Long totalVendas = saleRepository.count();
        Long vendasConcluidas = saleRepository.countByConcluida(true);
        report.put("totalVendas", totalVendas);
        report.put("vendasConcluidas", vendasConcluidas);
        report.put("vendasPendentes", totalVendas - vendasConcluidas);

        // Taxa de conversão
        double taxaConversao = totalVendas > 0 ? (vendasConcluidas * 100.0 / totalVendas) : 0;
        report.put("taxaConversao", Math.round(taxaConversao * 100.0) / 100.0);

        // Métricas de Saques
        Long totalSaques = withdrawalRepository.count();
        Long saquesPendentes = withdrawalRepository.countByStatus("PENDENTE");
        Long saquesAprovados = withdrawalRepository.countByStatus("APROVADO");
        Long saquesConcluidos = withdrawalRepository.countByStatus("CONCLUIDO");
        Long saquesRejeitados = withdrawalRepository.countByStatus("REJEITADO");

        report.put("totalSaques", totalSaques);
        report.put("saquesPendentes", saquesPendentes);
        report.put("saquesAprovados", saquesAprovados);
        report.put("saquesConcluidos", saquesConcluidos);
        report.put("saquesRejeitados", saquesRejeitados);

        // Métricas de Atividades (todas)
        Long totalAtividades = activityRepository.count();
        Long totalEmails = activityRepository.countByTipo(ActivityType.EMAIL);
        Long totalReunioes = activityRepository.countByTipo(ActivityType.REUNIAO);
        Long totalVisitas = activityRepository.countByTipo(ActivityType.VISITA);

        report.put("totalAtividades", totalAtividades);
        report.put("totalEmails", totalEmails);
        report.put("totalReunioes", totalReunioes);
        report.put("totalVisitas", totalVisitas);

        return report;
    }

    /**
     * Gera relatório detalhado de ligações
     */
    public Map<String, Object> getCallsReport() {
        Map<String, Object> report = new HashMap<>();

        Long totalLigacoes = activityRepository.countByTipo(ActivityType.LIGACAO);
        report.put("totalLigacoes", totalLigacoes);

        // Outras métricas podem ser adicionadas aqui

        return report;
    }

    /**
     * Gera relatório detalhado de vendas
     */
    public Map<String, Object> getSalesReport() {
        Map<String, Object> report = new HashMap<>();

        Long totalVendas = saleRepository.count();
        Long vendasConcluidas = saleRepository.countByConcluida(true);

        report.put("totalVendas", totalVendas);
        report.put("vendasConcluidas", vendasConcluidas);
        report.put("vendasPendentes", totalVendas - vendasConcluidas);

        // Taxa de conversão
        double taxaConversao = totalVendas > 0 ? (vendasConcluidas * 100.0 / totalVendas) : 0;
        report.put("taxaConversao", Math.round(taxaConversao * 100.0) / 100.0);

        return report;
    }

    /**
     * Gera relatório detalhado de saques
     */
    public Map<String, Object> getWithdrawalsReport() {
        Map<String, Object> report = new HashMap<>();

        Long totalSaques = withdrawalRepository.count();
        Long saquesPendentes = withdrawalRepository.countByStatus("PENDENTE");
        Long saquesAprovados = withdrawalRepository.countByStatus("APROVADO");
        Long saquesConcluidos = withdrawalRepository.countByStatus("CONCLUIDO");
        Long saquesRejeitados = withdrawalRepository.countByStatus("REJEITADO");

        report.put("totalSaques", totalSaques);
        report.put("saquesPendentes", saquesPendentes);
        report.put("saquesAprovados", saquesAprovados);
        report.put("saquesConcluidos", saquesConcluidos);
        report.put("saquesRejeitados", saquesRejeitados);

        return report;
    }
}
