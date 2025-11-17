package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/me")
public class UserPlanController {

    private final UserAccountService userAccountService;
    private final PartnerService partnerService;

    public UserPlanController(UserAccountService userAccountService,
                              PartnerService partnerService) {
        this.userAccountService = userAccountService;
        this.partnerService = partnerService;
    }

    @GetMapping("/plan")
    public String myPlan(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        model.addAttribute("pageTitle", "SUB - Meu Plano");

        return userAccountService.findByUsername(authentication.getName())
                .map(user -> partnerService.getPartnerByEmail(user.getEmail())
                        .map(partner -> populateWithPartnerData(model, partner))
                        .orElseGet(() -> {
                            model.addAttribute("missingPartner", true);
                            return "meu_plano";
                        }))
                .orElseGet(() -> {
                    model.addAttribute("missingUser", true);
                    return "meu_plano";
                });
    }

    private String populateWithPartnerData(Model model, Partner partner) {
        model.addAttribute("partner", partner);
        model.addAttribute("planName", "Proteção Veicular Completa");
        model.addAttribute("planCode", "ASSOC-PLANO-PADRAO");
        model.addAttribute("planLevel", "Cobertura Integral");
        model.addAttribute("planMonthlyValue", "R$ 249,90");
        model.addAttribute("planStatus", "Ativo");
        model.addAttribute("planRenewal", "15 de dezembro de 2024");
        model.addAttribute("planCoverageItems", List.of(
                "Proteção contra roubo e furto",
                "Assistência 24h com guincho até 400 km",
                "Cobertura contra fenômenos naturais",
                "Carro reserva por até 7 dias"
        ));
        model.addAttribute("planBenefits", List.of(
                "Atendimento prioritário com consultor dedicado",
                "Descontos em oficinas e serviços parceiros",
                "Programa de indicação com bônus na mensalidade"
        ));
        model.addAttribute("planAssistanceChannels", List.of(
                "Central 24h: 0800 123 4000",
                "WhatsApp: (11) 99999-0000",
                "E-mail: atendimento@sub.com.br"
        ));
        return "meu_plano";
    }
}
