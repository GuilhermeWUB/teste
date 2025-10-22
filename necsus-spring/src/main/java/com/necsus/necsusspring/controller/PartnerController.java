package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.CreatePartnerRequest;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/partners")
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    @GetMapping("/new")
    public String showCreatePartnerForm(Model model) {
        model.addAttribute("createPartnerRequest", new CreatePartnerRequest());
        return "cadastro_associado";
    }

    @PostMapping
    public String createPartner(CreatePartnerRequest request) {
        partnerService.createPartner(request.getPartner(), request.getAddress(), request.getAdhesion());
        return "redirect:/partners";
    }

    @GetMapping
    public String listPartners(Model model) {
        model.addAttribute("partners", partnerService.getAllPartners());
        return "lista_associados";
    }

    @GetMapping("/{id}")
    public String showPartnerDetails(@PathVariable Long id, Model model) {
        model.addAttribute("partner", partnerService.getPartnerById(id).orElse(null));
        return "detalhes_associado";
    }

    @GetMapping("/edit/{id}")
    public String showUpdatePartnerForm(@PathVariable Long id, Model model) {
        model.addAttribute("partner", partnerService.getPartnerById(id).orElse(null));
        return "update_associado";
    }

    @PostMapping("/update/{id}")
    public String updatePartner(@PathVariable Long id, Partner partner) {
        partner.setId(id);
        partnerService.updatePartner(partner);
        return "redirect:/partners";
    }

    @GetMapping("/delete/{id}")
    public String deletePartner(@PathVariable Long id) {
        partnerService.deletePartner(id);
        return "redirect:/partners";
    }
}
