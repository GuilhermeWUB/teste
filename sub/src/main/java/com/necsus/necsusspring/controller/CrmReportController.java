package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/crm/api/relatorios")
@PreAuthorize("hasAnyRole('ADMIN', 'COMERCIAL', 'CLOSERS', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class CrmReportController {

    private final ReportService reportService;

    public CrmReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Retorna relatório geral com métricas de ligações, vendas e saques
     */
    @GetMapping("/geral")
    public ResponseEntity<Map<String, Object>> getGeneralReport() {
        Map<String, Object> report = reportService.getGeneralReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Retorna relatório detalhado de ligações
     */
    @GetMapping("/ligacoes")
    public ResponseEntity<Map<String, Object>> getCallsReport() {
        Map<String, Object> report = reportService.getCallsReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Retorna relatório detalhado de vendas
     */
    @GetMapping("/vendas")
    public ResponseEntity<Map<String, Object>> getSalesReport() {
        Map<String, Object> report = reportService.getSalesReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Retorna relatório detalhado de saques
     */
    @GetMapping("/saques")
    public ResponseEntity<Map<String, Object>> getWithdrawalsReport() {
        Map<String, Object> report = reportService.getWithdrawalsReport();
        return ResponseEntity.ok(report);
    }
}
