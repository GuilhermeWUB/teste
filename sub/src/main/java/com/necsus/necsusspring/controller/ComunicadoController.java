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
        logger.info("Buscando associado para o usuário: {}", authentication.getName());
        Partner partner = getPartnerForUser(authentication);

        if (partner != null) {
            logger.info("Associado encontrado: ID={}, Nome={}, Email={}",
                       partner.getId(), partner.getName(), partner.getEmail());
        } else {
            logger.warn("ATENÇÃO: Nenhum associado encontrado para o usuário: {}. Verificando dados...",
                       authentication.getName());
            // Log adicional para diagnóstico
            userAccountService.findByUsername(authentication.getName()).ifPresentOrElse(
                user -> logger.warn("Usuário encontrado com email: {}. Nenhum associado cadastrado com este email.", user.getEmail()),
                () -> logger.error("Usuário {} não encontrado no sistema!", authentication.getName())
            );
        }

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

        // Preenche data e hora da comunicação automaticamente se não foram fornecidas
        if (event.getDataComunicacao() == null) {
            event.setDataComunicacao(java.time.LocalDate.now());
        }
        if (event.getHoraComunicacao() == null) {
            java.time.LocalTime agora = java.time.LocalTime.now();
            int horaFormatada = agora.getHour() * 100 + agora.getMinute();
            event.setHoraComunicacao(horaFormatada);
        }

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

    /**
     * Atualiza a descrição de um comunicado do Associado
     */
    @PostMapping("/update-description")
    public String updateDescription(@RequestParam Long eventId,
                                     @RequestParam String descricao,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) {
            logger.warn("Acesso negado para atualizar descrição. Apenas role USER/Associado tem acesso. Role atual: {}", userRole);
            return "redirect:/";
        }

        try {
            // Busca o associado do usuário logado
            Partner partner = getPartnerForUser(authentication);
            if (partner == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Associado não encontrado.");
                return "redirect:/comunicados";
            }

            // Busca o evento
            var eventOpt = eventService.findById(eventId);
            if (eventOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Evento não encontrado.");
                return "redirect:/comunicados";
            }

            Event event = eventOpt.get();

            // Valida que o evento pertence ao associado logado (segurança)
            if (event.getPartner() == null || !event.getPartner().getId().equals(partner.getId())) {
                logger.warn("Tentativa de edição não autorizada - Associado {} tentou editar evento {} de outro associado",
                        partner.getId(), eventId);
                redirectAttributes.addFlashAttribute("errorMessage", "Você não tem permissão para editar este evento.");
                return "redirect:/comunicados";
            }

            // Usa Map para atualização parcial, evitando problema de cache do JPA
            String modifiedBy = partner.getName() + " (Associado)";
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("descricao", descricao);

            eventService.updatePartialWithHistory(eventId, updates, modifiedBy);

            logger.info("Descrição do evento {} atualizada pelo associado {} (ID: {})", eventId, partner.getName(), partner.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Descrição atualizada com sucesso!");
            return "redirect:/comunicados";

        } catch (Exception ex) {
            logger.error("Erro ao atualizar descrição do evento {}: ", eventId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível atualizar a descrição. Tente novamente.");
            return "redirect:/comunicados";
        }
    }

    /**
     * Atualiza as observações de um comunicado do Associado
     */
    @PostMapping("/update-observations")
    public String updateObservations(@RequestParam Long eventId,
                                      @RequestParam(required = false) String observacoes,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) {
            logger.warn("Acesso negado para atualizar observações. Apenas role USER/Associado tem acesso. Role atual: {}", userRole);
            return "redirect:/";
        }

        try {
            // Busca o associado do usuário logado
            Partner partner = getPartnerForUser(authentication);
            if (partner == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Associado não encontrado.");
                return "redirect:/comunicados";
            }

            // Busca o evento
            var eventOpt = eventService.findById(eventId);
            if (eventOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Evento não encontrado.");
                return "redirect:/comunicados";
            }

            Event event = eventOpt.get();

            // Valida que o evento pertence ao associado logado (segurança)
            if (event.getPartner() == null || !event.getPartner().getId().equals(partner.getId())) {
                logger.warn("Tentativa de edição não autorizada - Associado {} tentou editar observações do evento {} de outro associado",
                        partner.getId(), eventId);
                redirectAttributes.addFlashAttribute("errorMessage", "Você não tem permissão para editar este evento.");
                return "redirect:/comunicados";
            }

            // Atualiza as observações usando updateWithHistory para rastrear mudanças
            event.setObservacoes(observacoes);
            String modifiedBy = partner.getName() + " (Associado)";
            eventService.updateWithHistory(eventId, event, modifiedBy);

            logger.info("Observações do evento {} atualizadas pelo associado {} (ID: {})", eventId, partner.getName(), partner.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Observações atualizadas com sucesso!");
            return "redirect:/comunicados";

        } catch (Exception ex) {
            logger.error("Erro ao atualizar observações do evento {}: ", eventId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível atualizar as observações. Tente novamente.");
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
