package com.necsus.necsusspring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalPartners", 128);
        model.addAttribute("activeVehicles", 54);
        model.addAttribute("pendingInvoices", 12);
        model.addAttribute("collectionProgress", 72);
        return "dashboard";
    }
}
