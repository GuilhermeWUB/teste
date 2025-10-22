package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.BankSlip;
import com.necsus.necsusspring.repository.BankSlipRepository;
import com.necsus.necsusspring.service.BoletoService;
import com.necsus.necsusspring.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Controller
@RequestMapping("/pagamentos")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BoletoService boletoService;

    @Autowired
    private BankSlipRepository bankSlipRepository;

    @GetMapping("/gerar-mensalidades")
    public String showGenerateInvoicesForm() {
        return "gerar_mensalidades";
    }

    @PostMapping("/gerar-mensalidades")
    public ResponseEntity<String> generateMonthlyInvoices(
            @RequestParam("vehicle_id") Long vehicleId,
            @RequestParam("qtd_boletos") int numberOfSlips) {
        try {
            paymentService.generateMonthlyInvoices(vehicleId, numberOfSlips);
            return ResponseEntity.ok("Invoices generated successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating invoices: " + e.getMessage());
        }
    }

    @GetMapping("/visualizar-boleto/{boletoId}")
    public ResponseEntity<byte[]> viewBoleto(@PathVariable("boletoId") Long boletoId) {
        try {
            BankSlip bankSlip = bankSlipRepository.findById(boletoId)
                    .orElseThrow(() -> new RuntimeException("Boleto not found"));
            byte[] boletoPdf = boletoService.generateBoleto(bankSlip);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "boleto.pdf");

            return new ResponseEntity<>(boletoPdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}