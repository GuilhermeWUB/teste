package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.CreatePartnerRequest;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/partners")
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/new")
    public String showCreatePartnerForm(Model model) {
        model.addAttribute("createPartnerRequest", new CreatePartnerRequest());
        return "cadastro_associado";
    }

    @PostMapping
    public String createPartner(
            @Valid CreatePartnerRequest request,
            BindingResult result,
            @RequestParam(value = "documents", required = false) MultipartFile[] documents) {
        if (result.hasErrors()) {
            return "cadastro_associado";
        }

        // Processar upload de documentos
        if (documents != null && documents.length > 0) {
            List<String> documentPaths = fileStorageService.storeFiles(documents);
            request.getPartner().setDocumentPaths(documentPaths);
        }

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
        Partner partner = partnerService.getPartnerById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partner not found"));
        model.addAttribute("partner", partner);
        return "detalhes_associado";
    }

    @GetMapping("/edit/{id}")
    public String showUpdatePartnerForm(@PathVariable Long id, Model model) {
        Partner partner = partnerService.getPartnerById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partner not found"));
        model.addAttribute("partner", partner);
        return "update_associado";
    }

    @PostMapping("/update/{id}")
    public String updatePartner(
            @PathVariable Long id,
            Partner partnerDetails,
            @RequestParam(value = "documents", required = false) MultipartFile[] documents) {

        Partner partnerToUpdate = partnerService.getPartnerById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partner not found"));

        partnerToUpdate.setName(partnerDetails.getName());
        partnerToUpdate.setDateBorn(partnerDetails.getDateBorn());
        partnerToUpdate.setEmail(partnerDetails.getEmail());
        partnerToUpdate.setCpf(partnerDetails.getCpf());
        partnerToUpdate.setPhone(partnerDetails.getPhone());
        partnerToUpdate.setCell(partnerDetails.getCell());
        partnerToUpdate.setRg(partnerDetails.getRg());
        partnerToUpdate.setFax(partnerDetails.getFax());

        List<String> existingDocs = partnerToUpdate.getDocumentPaths();
        if (existingDocs == null) {
            existingDocs = new ArrayList<>();
        }

        if (documents != null && documents.length > 0) {
            List<String> newDocPaths = fileStorageService.storeFiles(documents);
            existingDocs.addAll(newDocPaths);
        }
        partnerToUpdate.setDocumentPaths(existingDocs);

        partnerService.updatePartner(partnerToUpdate);
        return "redirect:/partners";
    }

    @PostMapping("/delete/{id}")
    public String deletePartner(@PathVariable Long id) {
        partnerService.deletePartner(id);
        return "redirect:/partners";
    }
}
