package com.necsus.necsusspring.dto;

import java.time.LocalDateTime;

public record CrmActivityResponse(
        Long id,
        String title,
        String description,
        String status,
        String type,
        String leadSource,
        String responsible,
        String city,
        String state,
        LocalDateTime dueAt,
        LocalDateTime createdAt
) {
}
