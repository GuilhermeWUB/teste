package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.DashboardSummary;
import com.necsus.necsusspring.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/dashboard"})
    public String dashboard(Model model) {
        DashboardSummary summary = dashboardService.loadSummary();

        model.addAttribute("pageTitle", "SUB - Dashboard");
        model.addAttribute("totalPartners", summary.totalPartners());
        model.addAttribute("activeVehicles", summary.activeVehicles());
        model.addAttribute("pendingInvoices", summary.pendingInvoices());
        model.addAttribute("collectionProgress", summary.collectionProgress());

        return "dashboard";
    }
}
