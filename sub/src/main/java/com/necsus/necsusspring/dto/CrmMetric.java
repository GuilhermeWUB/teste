package com.necsus.necsusspring.dto;

public record CrmMetric(
        String title,
        String value,
        String helper,
        String badge,
        String iconClass,
        String badgeClass
) {
}
