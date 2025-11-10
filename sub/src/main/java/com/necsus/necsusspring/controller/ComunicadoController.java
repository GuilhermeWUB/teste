package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.ComunicadoService;
import com.necsus.necsusspring.service.EventService;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.UserAccountService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/comunicados")
public class ComunicadoController {

    private static final Logger logger = LoggerFactory.getLogger(ComunicadoController.class);

    private final ComunicadoService comunicadoService;
    private final EventService eventService;
    private final PartnerService partnerService;
    private final UserAccountService userAccountService;

    public ComunicadoController(ComunicadoService comunicadoService,
                                EventService eventService,
                                PartnerService partnerService,
                                UserAccountService userAccountService) {
        this.comunicadoService = comunicadoService;
        this.eventService = eventService;
        this.partnerService = partnerService;
        this.userAccountService = userAccountService;
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
        return Status.values();
    }

    @ModelAttribute("prioridadeOptions")
    public Prioridade[] prioridadeOptions() {
        return Prioridade.values();
    }

    @GetMapping
    public String index(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String userRole = getUserRole(authentication);

        // Bloqueia acesso de usuários admin - apenas usuários Associado (USER) podem acessar
        if (!"USER".equals(userRole)) {
            logger.warn("Acesso negado aos comunicados. Apenas role USER/Associado tem acesso. Role atual: {}", userRole);
            return "redirect:/";
        }

        // Busca apenas comunicados visíveis (ativos e não expirados)
        List<Comunicado> comunicados = comunicadoService.listVisiveis();

        // Busca o parceiro associado ao usuário logado
        Partner partner = getPartnerForUser(authentication);

        // Busca os eventos criados pelo associado
        List<Event> userEvents = List.of();
        if (partner != null) {
            userEvents = eventService.listByPartnerId(partner.getId());
            logger.info("Encontrados {} eventos para o associado ID: {}", userEvents.size(), partner.getId());
        }

        // Cria um novo evento para o formulário
        Event event = new Event();
        event.setStatus(Status.ABERTO); // Status padrão
        if (partner != null) {
            event.setPartner(partner);
        } else {
            event.setPartner(new Partner());
        }

        model.addAttribute("pageTitle", "SUB - Comunicados");
        model.addAttribute("comunicados", comunicados);
        model.addAttribute("event", event);
        model.addAttribute("userPartner", partner);
        model.addAttribute("userEvents", userEvents);

        return "comunicados";
    }

    @PostMapping("/criar-evento")
    public String createEvent(@Valid @ModelAttribute("event") Event event,
                              BindingResult result,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes,
                              Model model) {

        if (authentication == null) {
            return "redirect:/login";
        }

        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) {
            logger.warn("Acesso negado para criar evento. Apenas role USER/Associado tem acesso. Role atual: {}", userRole);
            return "redirect:/";
        }

        // Busca o parceiro associado ao usuário logado
        Partner partner = getPartnerForUser(authentication);

        if (partner == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível localizar seus dados de associado. Entre em contato com o administrador.");
            return "redirect:/comunicados";
        }

        // Define o parceiro automaticamente como o usuário logado
        event.setPartner(partner);

        // Validações manuais
        if (event.getPartner() == null || event.getPartner().getId() == null) {
            result.rejectValue("partner", "NotNull", "Erro ao identificar o associado");
        }

        if (result.hasErrors()) {
            model.addAttribute("comunicados", comunicadoService.listVisiveis());
            model.addAttribute("userPartner", partner);
            model.addAttribute("userEvents", eventService.listByPartnerId(partner.getId()));
            return "comunicados";
        }

        try {
            eventService.create(event);
            logger.info("Evento criado com sucesso pelo usuário Associado: {}", authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Evento cadastrado com sucesso! Nossa equipe entrará em contato em breve.");
            return "redirect:/comunicados";
        } catch (Exception ex) {
            logger.error("Erro ao criar evento pelo usuário Associado: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Não foi possível salvar o evento. Tente novamente.");
            return "redirect:/comunicados";
        }
    }

    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                .findFirst()
                .orElse("USER");
    }

    private Partner getPartnerForUser(Authentication authentication) {
        return userAccountService.findByUsername(authentication.getName())
                .flatMap(user -> partnerService.getPartnerByEmail(user.getEmail()))
                .orElse(null);
    }
}
