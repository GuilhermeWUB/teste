package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.service.ReportDataService;
import com.necsus.necsusspring.service.ReportPdfService;
import com.necsus.necsusspring.service.ReportDataService.PartnerReportData;
import com.necsus.necsusspring.service.ReportDataService.VehicleReportData;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportDataService reportDataService;
    private final ReportPdfService reportPdfService;

    public ReportController(ReportDataService reportDataService, ReportPdfService reportPdfService) {
        this.reportDataService = reportDataService;
        this.reportPdfService = reportPdfService;
    }

    @GetMapping
    public String redirectToDefault() {
        return "redirect:/reports/vehicles";
    }

    @GetMapping("/vehicles")
    public String vehicleReport(Model model) {
        VehicleReportData reportData = reportDataService.loadVehicleReportData();
        model.addAttribute("pageTitle", "SUB - Relatório de Veículos");
        model.addAttribute("hasVehicles", reportData.hasVehicles());
        model.addAttribute("vehicles", reportData.vehicles());

        if (!reportData.hasVehicles()) {
            model.addAttribute("errorMessage", "Não há veículos cadastrados para gerar o relatório.");
            return "relatorio_veiculos";
        }

        model.addAttribute("totalVehicles", reportData.totalVehicles());
        model.addAttribute("distinctPartners", reportData.distinctPartners());
        model.addAttribute("totalMonthlyValue", reportData.totalMonthlyValue());
        model.addAttribute("totalMonthlyValueFormatted", reportData.totalMonthlyValueFormatted());
        model.addAttribute("vehiclesByFuel", reportData.vehiclesByFuel());
        model.addAttribute("vehiclesByMaker", reportData.vehiclesByMaker());

        return "relatorio_veiculos";
    }

    @GetMapping("/partners")
    public String partnerReport(Model model) {
        PartnerReportData reportData = reportDataService.loadPartnerReportData();
        model.addAttribute("pageTitle", "SUB - Relatório de Associados");
        model.addAttribute("hasPartners", reportData.hasPartners());
        model.addAttribute("partners", reportData.partners());

        if (!reportData.hasPartners()) {
            model.addAttribute("errorMessage", "Não há associados cadastrados para gerar o relatório.");
            return "relatorio_associados";
        }

        model.addAttribute("totalPartners", reportData.totalPartners());
        model.addAttribute("partnersWithVehicles", reportData.partnersWithVehicles());
        model.addAttribute("partnersWithoutVehicles", reportData.partnersWithoutVehicles());
        model.addAttribute("totalVehicles", reportData.totalVehicles());
        model.addAttribute("partnersByCity", reportData.partnersByCity());
        model.addAttribute("averageVehiclesPerPartner", reportData.averageVehiclesPerPartner());
        model.addAttribute("partnerVehicleCount", reportData.partnerVehicleCount());
        model.addAttribute("partnerAddressSummary", reportData.partnerAddressSummary());

        return "relatorio_associados";
    }

    @PostMapping("/partners/pdf")
    public ResponseEntity<byte[]> partnerReportPdf(@RequestParam(value = "fields", required = false) List<String> fields) {
        PartnerReportData reportData = reportDataService.loadPartnerReportData();
        if (!reportData.hasPartners()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        byte[] pdf = reportPdfService.generatePartnerReport(reportData, fields);
        return buildPdfResponse(pdf, "relatorio-associados.pdf");
    }

    @PostMapping("/vehicles/pdf")
    public ResponseEntity<byte[]> vehicleReportPdf(@RequestParam(value = "fields", required = false) List<String> fields) {
        VehicleReportData reportData = reportDataService.loadVehicleReportData();
        if (!reportData.hasVehicles()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        byte[] pdf = reportPdfService.generateVehicleReport(reportData, fields);
        return buildPdfResponse(pdf, "relatorio-veiculos.pdf");
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.length);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        headers.setContentDisposition(contentDisposition);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}

