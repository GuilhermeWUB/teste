package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.BankSlip;
import com.necsus.necsusspring.model.BankShipment;
import com.necsus.necsusspring.repository.BankSlipRepository;
import com.necsus.necsusspring.service.BoletoService;
import com.necsus.necsusspring.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

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
        Vehicle vehicle = vehicleRepository.findWithPartnerAndPaymentById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid vehicle Id:" + vehicleId));
        model.addAttribute("vehicle", vehicle);
        return "gerar_mensalidades";
    }

    @PostMapping("/gerar-mensalidades")
    public String generateMonthlyInvoices(
            @RequestParam("vehicle_id") Long vehicleId,
            @RequestParam("qtd_boletos") int numberOfSlips,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            BankShipment bankShipment = paymentService.generateMonthlyInvoices(vehicleId, numberOfSlips);
            // Redireciona para visualizar as faturas geradas
            return "redirect:/pagamentos/faturas-geradas?bankShipmentId=" + bankShipment.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao gerar faturas: " + e.getMessage());
            return "redirect:/pagamentos/gerar-mensalidades?vehicle_id=" + vehicleId;
        }
    }

    @GetMapping("/faturas-geradas")
    public String showGeneratedInvoices(@RequestParam("bankShipmentId") Long bankShipmentId, Model model) {
        List<BankSlip> invoices = paymentService.findInvoicesByBankShipment(bankShipmentId);

        if (invoices.isEmpty()) {
            model.addAttribute("errorMessage", "Nenhuma fatura encontrada.");
            return "redirect:/partners";
        }

        // Pega o primeiro boleto para obter informações do veículo e associado
        BankSlip firstInvoice = invoices.get(0);
        Vehicle vehicle = firstInvoice.getBankShipment().getVehicle();
        Partner partner = firstInvoice.getPartner();

        model.addAttribute("invoices", invoices);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("partner", partner);
        model.addAttribute("bankShipmentId", bankShipmentId);

        return "faturas_geradas";
    }

    @PostMapping("/marcar-paga/{bankSlipId}")
    public String markInvoiceAsPaid(
            @PathVariable Long bankSlipId,
            @RequestParam(required = false) BigDecimal valorRecebido,
            @RequestParam("bankShipmentId") Long bankShipmentId,
            RedirectAttributes redirectAttributes) {
        try {
            BankSlip bankSlip = paymentService.markAsPaid(bankSlipId, valorRecebido);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Fatura #" + bankSlip.getId() + " marcada como paga com sucesso!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erro ao marcar fatura como paga: " + e.getMessage());
        }
        return "redirect:/pagamentos/faturas-geradas?bankShipmentId=" + bankShipmentId;
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

    /**
     * Página que lista todos os boletos gerados
     */
    @GetMapping("/boletos")
    public String listAllInvoices(Model model) {
        List<BankSlip> pendingInvoices = paymentService.listPendingInvoices();
        List<BankSlip> paidInvoices = paymentService.listPaidInvoices();

        // Calcula totais financeiros para indicadores
        BigDecimal totalValuePending = pendingInvoices.stream()
                .map(BankSlip::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalValuePaid = paidInvoices.stream()
                .map(b -> b.getValorRecebido() != null ? b.getValorRecebido() : b.getValor())
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("pendingInvoices", pendingInvoices);
        model.addAttribute("paidInvoices", paidInvoices);
        model.addAttribute("totalPending", pendingInvoices.size());
        model.addAttribute("totalPaid", paidInvoices.size());
        model.addAttribute("totalValuePending", totalValuePending);
        model.addAttribute("totalValuePaid", totalValuePaid);

        return "boletos";
    }

    /**
     * Marcar boleto como pago na página de todos os boletos
     */
    @PostMapping("/boletos/marcar-paga/{bankSlipId}")
    public String markInvoiceAsPaidFromList(
            @PathVariable Long bankSlipId,
            @RequestParam(required = false) BigDecimal valorRecebido,
            RedirectAttributes redirectAttributes) {
        try {
            BankSlip bankSlip = paymentService.markAsPaid(bankSlipId, valorRecebido);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Boleto #" + bankSlip.getId() + " marcado como pago com sucesso!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erro ao marcar boleto como pago: " + e.getMessage());
        }
        return "redirect:/pagamentos/boletos";
    }

    /**
     * Cancelar/apagar um boleto
     */
    @PostMapping("/boletos/cancelar/{bankSlipId}")
    public String cancelInvoice(
            @PathVariable Long bankSlipId,
            RedirectAttributes redirectAttributes) {
        try {
            paymentService.cancelInvoice(bankSlipId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Boleto #" + bankSlipId + " cancelado com sucesso!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erro ao cancelar boleto: " + e.getMessage());
        }
        return "redirect:/pagamentos/boletos";
    }
}