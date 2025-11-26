package com.necsus.necsusspring.dto;

import java.util.Map;

public record DashboardMetrics(
    // Métricas de Vendas
    Long totalVendas,
    Long vendasConcluidas,
    Long vendasEmNegociacao,
    Double totalReceita,
    Double receitaMesAtual,
    Map<String, Long> vendasPorStatus,

    // Métricas de Atividades
    Long totalAtividades,
    Long atividadesAgendadas,
    Long atividadesConcluidas,
    Long atividadesHoje,
    Map<String, Long> atividadesPorTipo,
    Map<String, Long> atividadesPorStatus,

    // Taxa de Conversão
    Double taxaConversao
) {}
