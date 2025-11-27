package com.necsus.necsusspring.dto;

import java.time.LocalDateTime;

public record RegionalResponse(
    Long id,
    String name,
    String code,
    String description,
    Boolean active,
    LocalDateTime createdAt
) {}
