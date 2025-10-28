package com.necsus.necsusspring.controller;

import com.itextpdf.text.DocumentException;
import com.necsus.necsusspring.dto.ReportConfig;
import com.necsus.necsusspring.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/vehicles")
    public String vehicleReportPage(Model model) {
        Map<String, String> fieldLabels = reportService.getVehicleFieldLabels();
        model.addAttribute("fieldLabels", fieldLabels);
        return "relatorio_veiculos";
    }

    @GetMapping("/partners")
    public String partnerReportPage(Model model) {
        Map<String, String> fieldLabels = reportService.getPartnerFieldLabels();
        model.addAttribute("fieldLabels", fieldLabels);
        return "relatorio_associados";
    }

    @PostMapping("/vehicles/generate")
    public ResponseEntity<byte[]> generateVehicleReport(@RequestBody ReportConfig config) {
        try {
            byte[] reportData = reportService.generateVehicleReport(config);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            if ("pdf".equalsIgnoreCase(config.getFormat())) {
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "relatorio_veiculos_" + timestamp + ".pdf");
            } else {
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", "relatorio_veiculos_" + timestamp + ".xlsx");
            }

            return new ResponseEntity<>(reportData, headers, HttpStatus.OK);
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/partners/generate")
    public ResponseEntity<byte[]> generatePartnerReport(@RequestBody ReportConfig config) {
        try {
            byte[] reportData = reportService.generatePartnerReport(config);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            if ("pdf".equalsIgnoreCase(config.getFormat())) {
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "relatorio_associados_" + timestamp + ".pdf");
            } else {
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", "relatorio_associados_" + timestamp + ".xlsx");
            }

            return new ResponseEntity<>(reportData, headers, HttpStatus.OK);
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
