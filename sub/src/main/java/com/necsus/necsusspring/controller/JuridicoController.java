package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.BankSlip;
import com.necsus.necsusspring.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller para gerenciar as páginas do módulo Jurídico
 * Acessível apenas para administradores
 */
@Controller
@RequestMapping("/juridico")
@PreAuthorize("hasRole('ADMIN')")
public class JuridicoController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Página de Cobrança (Kanban de processos)
     */
    @GetMapping(value = "/cobranca", produces = MediaType.TEXT_HTML_VALUE)
    public String cobranca(Model model) {
        model.addAttribute("pageTitle", "Processos em Geral - Jurídico");
        return "juridico/cobranca";
    }

    /**
     * Página de Gestão de Faturas - Lista todas as faturas pendentes e pagas
     */
    @GetMapping(value = "/faturas", produces = MediaType.TEXT_HTML_VALUE)
    public String faturas(Model model) {
        List<BankSlip> pendingInvoices = paymentService.listPendingInvoices();
        List<BankSlip> paidInvoices = paymentService.listPaidInvoices();

        model.addAttribute("pageTitle", "Gestão de Faturas - Jurídico");
        model.addAttribute("pendingInvoices", pendingInvoices);
        model.addAttribute("paidInvoices", paidInvoices);
        return "juridico/faturas";
    }

    /**
     * Página de Cobrança Fidelidade
     */
    @GetMapping(value = "/cobranca-fidelidade", produces = MediaType.TEXT_HTML_VALUE)
    public String cobrancaFidelidade(Model model) {
        model.addAttribute("pageTitle", "Cobrança Fidelidade - Jurídico");
        return "juridico/cobranca_fidelidade";
    }

    /**
     * Página de Processos em Geral
     */
    @GetMapping(value = "/processos", produces = MediaType.TEXT_HTML_VALUE)
    public String processos(Model model) {
        model.addAttribute("pageTitle", "Processos em Geral - Jurídico");
        return "juridico/processos";
    }

    /**
     * Página de Acordos a pagar
     */
    @GetMapping(value = "/acordos", produces = MediaType.TEXT_HTML_VALUE)
    public String acordos(Model model) {
        model.addAttribute("pageTitle", "Acordos a pagar - Jurídico");
        return "juridico/acordos";
    }

    /**
     * Endpoint para marcar uma fatura como paga (apenas admin)
     */
    @PostMapping("/marcar-paga/{bankSlipId}")
    public String markInvoiceAsPaid(
            @PathVariable Long bankSlipId,
            @RequestParam(required = false) BigDecimal valorRecebido,
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
        return "redirect:/juridico/cobranca";
    }

    /**
     * Endpoint para visualizar detalhes de uma fatura específica
     */
    @GetMapping("/fatura/{bankSlipId}")
    public String viewInvoiceDetails(@PathVariable Long bankSlipId, Model model) {
        try {
            BankSlip bankSlip = paymentService.findById(bankSlipId);
            model.addAttribute("bankSlip", bankSlip);
            model.addAttribute("pageTitle", "Detalhes da Fatura #" + bankSlipId);
            return "juridico/fatura_detalhes";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fatura não encontrada: " + e.getMessage());
            return "redirect:/juridico/cobranca";
        }
    }
}
