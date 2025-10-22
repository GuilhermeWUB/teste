package com.example.necsusspring.controller;

import com.example.necsusspring.dto.CreatePartnerRequest;
import com.example.necsusspring.model.Partner;
import com.example.necsusspring.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
