package com.necsus.necsusspring.dto;

import java.util.List;

public record CrmKanbanColumn(
    String name,
    String badgeClass,
    String badge,
    String helper,
    List<CrmKanbanDeal> deals
) {
    public record CrmKanbanDeal(
        String stage,
        String title,
        String company,
        String status,
        String amount,
        String dueDate
    ) {}
}
