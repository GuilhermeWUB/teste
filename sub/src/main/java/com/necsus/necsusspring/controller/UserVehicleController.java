package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.UserAccountService;
import com.necsus.necsusspring.service.VehicleService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/me")
public class UserVehicleController {

    private final UserAccountService userAccountService;
    private final PartnerService partnerService;
    private final VehicleService vehicleService;

    public UserVehicleController(UserAccountService userAccountService,
                                 PartnerService partnerService,
                                 VehicleService vehicleService) {
        this.userAccountService = userAccountService;
        this.partnerService = partnerService;
        this.vehicleService = vehicleService;
    }

    @GetMapping("/vehicles")
    public String myVehicles(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        model.addAttribute("pageTitle", "SUB - Meus Veículos");

        return userAccountService.findByUsername(authentication.getName())
                .map(user -> partnerService.getPartnerByEmail(user.getEmail())
                        .map(partner -> populateWithPartnerData(model, partner))
                        .orElseGet(() -> {
                            model.addAttribute("missingPartner", true);
                            return "meus_veiculos";
                        }))
                .orElseGet(() -> {
                    model.addAttribute("missingUser", true);
                    return "meus_veiculos";
                });
    }

    private String populateWithPartnerData(Model model, Partner partner) {
        List<Vehicle> vehicles = vehicleService.listByPartnerId(partner.getId());
        model.addAttribute("partner", partner);
        model.addAttribute("vehicles", vehicles);
        return "meus_veiculos";
    }
}
