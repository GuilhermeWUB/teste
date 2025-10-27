package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Payment;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.VehicleService;
import com.necsus.necsusspring.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final PartnerService partnerService;
    private final FileStorageService fileStorageService;

    public VehicleController(VehicleService vehicleService, PartnerService partnerService, FileStorageService fileStorageService) {
        this.vehicleService = vehicleService;
        this.partnerService = partnerService;
        this.fileStorageService = fileStorageService;
    }

    @ModelAttribute("partners")
    public List<Partner> partners() {
        return partnerService.getAllPartners();
    }

    @GetMapping
    public String listVehicles(@RequestParam(value = "partnerId", required = false) Long partnerId,
                               Model model) {
        List<Vehicle> vehicles = vehicleService.listAll(partnerId);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("partnerId", partnerId);
        if (partnerId != null) {
            partnerService.getPartnerById(partnerId)
                    .ifPresent(partner -> model.addAttribute("selectedPartner", partner));
        }
        return "lista_veiculos";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(value = "partnerId", required = false) Long partnerId,
                                 Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPayment(new Payment());
        if (partnerId != null) {
            vehicle.setPartnerId(partnerId);
        }
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("partnerId", partnerId);
        return "cadastro_veiculo";
    }

    @PostMapping
    public String createVehicle(@Valid @ModelAttribute("vehicle") Vehicle vehicle,
                                BindingResult result,
                                @RequestParam(value = "inspectionPhotos", required = false) MultipartFile[] inspectionPhotos,
                                @RequestParam(value = "documentPhotos", required = false) MultipartFile[] documentPhotos,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (vehicle.getPayment() == null) {
            vehicle.setPayment(new Payment());
        }
        if (result.hasErrors()) {
            model.addAttribute("partnerId", vehicle.getPartnerId());
            return "cadastro_veiculo";
        }

        // Processar upload de fotos de inspeção
        if (inspectionPhotos != null && inspectionPhotos.length > 0) {
            List<String> inspectionPaths = fileStorageService.storeFiles(inspectionPhotos);
            vehicle.setInspectionPhotoPaths(inspectionPaths);
        }

        // Processar upload de fotos de documentação
        if (documentPhotos != null && documentPhotos.length > 0) {
            List<String> documentPaths = fileStorageService.storeFiles(documentPhotos);
            vehicle.setDocumentPhotoPaths(documentPaths);
        }

        Vehicle savedVehicle = vehicleService.create(vehicle);
        redirectAttributes.addFlashAttribute("successMessage", "Veículo cadastrado com sucesso!");
        return buildRedirect(savedVehicle.getPartnerId());
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @RequestParam(value = "partnerId", required = false) Long partnerId,
                               Model model) {
        Vehicle vehicle = vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (vehicle.getPayment() == null) {
            vehicle.setPayment(new Payment());
        }
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("partnerId", partnerId != null ? partnerId : vehicle.getPartnerId());
        return "update_veiculo";
    }

    @PostMapping("/update/{id}")
    public String updateVehicle(@PathVariable Long id,
                                @Valid @ModelAttribute("vehicle") Vehicle vehicle,
                                BindingResult result,
                                @RequestParam(value = "inspectionPhotos", required = false) MultipartFile[] inspectionPhotos,
                                @RequestParam(value = "documentPhotos", required = false) MultipartFile[] documentPhotos,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (vehicle.getPayment() == null) {
            vehicle.setPayment(new Payment());
        }
        if (result.hasErrors()) {
            model.addAttribute("partnerId", vehicle.getPartnerId());
            return "update_veiculo";
        }

        // Buscar veículo existente para manter fotos antigas
        Vehicle existingVehicle = vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        // Adicionar novas fotos de inspeção às existentes
        List<String> existingInspectionPhotos = existingVehicle.getInspectionPhotoPaths();
        if (existingInspectionPhotos == null) {
            existingInspectionPhotos = new ArrayList<>();
        }
        if (inspectionPhotos != null && inspectionPhotos.length > 0) {
            List<String> newInspectionPaths = fileStorageService.storeFiles(inspectionPhotos);
            existingInspectionPhotos.addAll(newInspectionPaths);
        }
        vehicle.setInspectionPhotoPaths(existingInspectionPhotos);

        // Adicionar novas fotos de documentação às existentes
        List<String> existingDocumentPhotos = existingVehicle.getDocumentPhotoPaths();
        if (existingDocumentPhotos == null) {
            existingDocumentPhotos = new ArrayList<>();
        }
        if (documentPhotos != null && documentPhotos.length > 0) {
            List<String> newDocumentPaths = fileStorageService.storeFiles(documentPhotos);
            existingDocumentPhotos.addAll(newDocumentPaths);
        }
        vehicle.setDocumentPhotoPaths(existingDocumentPhotos);

        vehicle.setId(id);
        Vehicle updatedVehicle = vehicleService.update(id, vehicle);
        redirectAttributes.addFlashAttribute("successMessage", "Veículo atualizado com sucesso!");
        return buildRedirect(updatedVehicle.getPartnerId());
    }

    @PostMapping("/delete/{id}")
    public String deleteVehicle(@PathVariable Long id,
                                @RequestParam(value = "partnerId", required = false) Long partnerId,
                                RedirectAttributes redirectAttributes) {
        vehicleService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Veículo removido com sucesso!");
        return buildRedirect(partnerId);
    }

    private String buildRedirect(Long partnerId) {
        if (partnerId != null) {
            return "redirect:/vehicles?partnerId=" + partnerId;
        }
        return "redirect:/vehicles";
    }
}
