package com.necsus.necsusspring.dto;

public record DashboardSummary(long totalPartners, long activeVehicles, long pendingInvoices, int collectionProgress) {
}
