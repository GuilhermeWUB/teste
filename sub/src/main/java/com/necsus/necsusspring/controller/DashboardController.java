package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.DashboardSummary;
import com.necsus.necsusspring.model.Demand;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.service.DashboardService;
import com.necsus.necsusspring.service.DemandService;
import com.necsus.necsusspring.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final DemandService demandService;
    private final UserAccountService userAccountService;

    private static final int NEXT_DEMANDS_LIMIT = 3;

    public DashboardController(DashboardService dashboardService,
                               DemandService demandService,
                               UserAccountService userAccountService) {
        this.dashboardService = dashboardService;
        this.demandService = demandService;
        this.userAccountService = userAccountService;
    }

    @GetMapping({"/dashboard"})
    public String dashboard(Model model, Authentication authentication) {
        DashboardSummary summary = dashboardService.loadSummary();

        model.addAttribute("pageTitle", "SUB - Dashboard");
        model.addAttribute("totalPartners", summary.totalPartners());
        model.addAttribute("activeVehicles", summary.activeVehicles());
        model.addAttribute("pendingInvoices", summary.pendingInvoices());
        model.addAttribute("collectionProgress", summary.collectionProgress());
        model.addAttribute("nextDemands", loadNextDemands(authentication));

        return "dashboard";
    }

    private List<Demand> loadNextDemands(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }

        Optional<UserAccount> currentUser = userAccountService.findByUsername(username);
        return currentUser
                .map(user -> demandService.findNextDemandsForUser(user, NEXT_DEMANDS_LIMIT))
                .orElse(Collections.emptyList());
    }
}
