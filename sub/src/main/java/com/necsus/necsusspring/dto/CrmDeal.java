package com.necsus.necsusspring.dto;

public record CrmDeal(
        String title,
        String company,
        String amount,
        String stage,
        String dueDate,
        String status
) {
}
