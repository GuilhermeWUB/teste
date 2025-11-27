package com.necsus.necsusspring.dto;

import java.time.LocalDateTime;

public record CompanyUserResponse(
    Long id,
    String fullName,
    String username,
    String email,
    String role,
    String roleDisplayName,
    LocalDateTime createdAt,
    Boolean active
) {}
