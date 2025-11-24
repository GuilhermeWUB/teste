package com.necsus.necsusspring.dto;

public record CrmActivity(
        String title,
        String owner,
        String dueDate,
        String channel
) {
}
