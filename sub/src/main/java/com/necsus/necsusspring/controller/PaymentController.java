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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pagamentos")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BoletoService boletoService;

    @Autowired
    private BankSlipRepository bankSlipRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @GetMapping("/gerar-mensalidades")
    public String showGenerateInvoicesForm(@RequestParam("vehicle_id") Long vehicleId, Model model) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid vehicle Id:" + vehicleId));
        model.addAttribute("vehicle", vehicle);
        return "gerar_mensalidades";
    }

    @PostMapping("/gerar-mensalidades")
    public String generateMonthlyInvoices(
            @RequestParam("vehicle_id") Long vehicleId,
            @RequestParam("qtd_boletos") int numberOfSlips,
            RedirectAttributes redirectAttributes) {
        try {
            Partner partner = paymentService.generateMonthlyInvoices(vehicleId, numberOfSlips);
            redirectAttributes.addFlashAttribute("successMessage", "Faturas geradas com sucesso!");
            return "redirect:/partners/" + partner.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao gerar faturas: " + e.getMessage());
            return "redirect:/pagamentos/gerar-mensalidades?vehicle_id=" + vehicleId;
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