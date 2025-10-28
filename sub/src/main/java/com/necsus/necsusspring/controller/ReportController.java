package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.ReportConfig;
import com.necsus.necsusspring.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
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

    @PostMapping({"/vehicles", "/vehicles/generate"})
    public ResponseEntity<byte[]> generateVehicleReport(@RequestBody ReportConfig config) {
        try {
            logger.info("Gerando relatório de veículos - Formato: {}, Campos: {}",
                       config.getFormat(), config.getSelectedFields());

            byte[] reportData = reportService.generateVehicleReport(config);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            if ("pdf".equalsIgnoreCase(config.getFormat())) {
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "relatorio_veiculos_" + timestamp + ".pdf");
                logger.info("Relatório PDF de veículos gerado com sucesso");
            } else {
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", "relatorio_veiculos_" + timestamp + ".xlsx");
                logger.info("Relatório Excel de veículos gerado com sucesso");
            }

            return new ResponseEntity<>(reportData, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erro ao gerar relatório de veículos: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping({"/partners", "/partners/generate"})
    public ResponseEntity<byte[]> generatePartnerReport(@RequestBody ReportConfig config) {
        try {
            logger.info("Gerando relatório de associados - Formato: {}, Campos: {}",
                       config.getFormat(), config.getSelectedFields());

            byte[] reportData = reportService.generatePartnerReport(config);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            if ("pdf".equalsIgnoreCase(config.getFormat())) {
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "relatorio_associados_" + timestamp + ".pdf");
                logger.info("Relatório PDF de associados gerado com sucesso");
            } else {
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", "relatorio_associados_" + timestamp + ".xlsx");
                logger.info("Relatório Excel de associados gerado com sucesso");
            }

            return new ResponseEntity<>(reportData, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erro ao gerar relatório de associados: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
