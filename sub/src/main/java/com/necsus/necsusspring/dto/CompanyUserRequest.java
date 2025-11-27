package com.necsus.necsusspring.dto;

public record CompanyUserRequest(
    String fullName,
    String username,
    String email,
    String password,
    String role,
    Boolean active
) {}
