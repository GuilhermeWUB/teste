package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.EventService;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/me")
public class UserCommunicationController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserCommunicationController.class);

    private final UserAccountService userAccountService;
    private final PartnerService partnerService;
    private final EventService eventService;

    public UserCommunicationController(UserAccountService userAccountService,
                                       PartnerService partnerService,
                                       EventService eventService) {
        this.userAccountService = userAccountService;
        this.partnerService = partnerService;
        this.eventService = eventService;
    }

    @ModelAttribute("motivoOptions")
    public Motivo[] motivoOptions() {
        return Motivo.values();
    }

    @ModelAttribute("envolvimentoOptions")
    public Envolvimento[] envolvimentoOptions() {
        return Envolvimento.values();
    }

    @ModelAttribute("statusOptions")
    public Status[] statusOptions() {
        // Retorna apenas os status permitidos para o usuário Associado criar
        return new Status[]{
            Status.COMUNICADO,
            Status.ABERTO
        };
    }

    @ModelAttribute("prioridadeOptions")
    public Prioridade[] prioridadeOptions() {
        return Prioridade.values();
    }

    /**
     * Exibe o formulário de cadastro de comunicado e lista os comunicados do Associado
     */
    @GetMapping("/comunicado")
    public String showCommunicationForm(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        model.addAttribute("pageTitle", "SUB - Meus Comunicados");

        return userAccountService.findByUsername(authentication.getName())
                .map(user -> partnerService.getPartnerByEmail(user.getEmail())
                        .map(partner -> {
                            Event event = new Event();
                            event.setStatus(Status.COMUNICADO); // Status padrão para Associados
                            event.setPartner(partner); // Pre-preenche com o associado logado

                            // Busca todos os eventos criados por este associado
                            java.util.List<Event> myEvents = eventService.listByPartnerId(partner.getId());
                            logger.info("Associado {} (ID: {}) possui {} eventos", partner.getName(), partner.getId(), myEvents.size());

                            model.addAttribute("event", event);
                            model.addAttribute("partner", partner);
                            model.addAttribute("readOnlyPartner", true); // Flag para desabilitar seleção de associado
                            model.addAttribute("myEvents", myEvents); // Lista de eventos do associado

                            return "cadastro_comunicado";
                        })
                        .orElseGet(() -> {
                            model.addAttribute("error", "Seu usuário não está vinculado a um associado. Entre em contato com o administrador.");
                            return "error";
                        }))
                .orElseGet(() -> {
                    model.addAttribute("error", "Usuário não encontrado. Faça login novamente.");
                    return "error";
                });
    }

    /**
     * Processa o cadastro de comunicado do Associado
     */
    @PostMapping("/comunicado")
    public String createCommunication(@Valid @ModelAttribute("event") Event event,
                                      BindingResult result,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        // Busca o associado do usuário logado
        var userOpt = userAccountService.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            model.addAttribute("formError", "Usuário não encontrado.");
            return "cadastro_comunicado";
        }

        var partnerOpt = partnerService.getPartnerByEmail(userOpt.get().getEmail());
        if (partnerOpt.isEmpty()) {
            model.addAttribute("formError", "Seu usuário não está vinculado a um associado.");
            return "cadastro_comunicado";
        }

        Partner partner = partnerOpt.get();

        // Força o partner do usuário logado (segurança)
        event.setPartner(partner);

        // Valida que o status seja apenas COMUNICADO ou ABERTO
        if (event.getStatus() != Status.COMUNICADO && event.getStatus() != Status.ABERTO) {
            result.rejectValue("status", "Invalid", "Status inválido para associados");
        }

        if (result.hasErrors()) {
            model.addAttribute("partner", partner);
            model.addAttribute("readOnlyPartner", true);
            return "cadastro_comunicado";
        }

        try {
            eventService.create(event);
            logger.info("Comunicado criado com sucesso pelo associado: {} (ID: {})", partner.getName(), partner.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Comunicado cadastrado com sucesso! Aguarde o contato do administrador.");
            return "redirect:/me/comunicado";
        } catch (Exception ex) {
            logger.error("Erro ao criar comunicado para associado {}: ", partner.getName(), ex);
            model.addAttribute("formError", ex.getMessage() != null ? ex.getMessage() : "Não foi possível salvar o comunicado. Tente novamente.");
            model.addAttribute("partner", partner);
            model.addAttribute("readOnlyPartner", true);
            return "cadastro_comunicado";
        }
    }
}
