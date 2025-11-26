package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.DashboardMetrics;
import com.necsus.necsusspring.service.CrmDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/crm/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'COMERCIAL', 'CLOSERS', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class CrmDashboardController {

    private final CrmDashboardService dashboardService;

    public CrmDashboardController(CrmDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetrics> getDashboardMetrics() {
        DashboardMetrics metrics = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/vendas")
    public ResponseEntity<Map<String, Object>> getSalesMetrics() {
        Map<String, Object> metrics = dashboardService.getSalesMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/atividades")
    public ResponseEntity<Map<String, Object>> getActivitiesMetrics() {
        Map<String, Object> metrics = dashboardService.getActivitiesMetrics();
        return ResponseEntity.ok(metrics);
    }
}
