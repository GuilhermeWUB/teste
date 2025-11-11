package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.EventService;
import com.necsus.necsusspring.service.FileStorageService;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.UserAccountService;
import com.necsus.necsusspring.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/me")
public class UserCommunicationController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserCommunicationController.class);

    private final UserAccountService userAccountService;
    private final PartnerService partnerService;
    private final EventService eventService;
    private final VehicleService vehicleService;
    private final FileStorageService fileStorageService;

    public UserCommunicationController(UserAccountService userAccountService,
                                       PartnerService partnerService,
                                       EventService eventService,
                                       VehicleService vehicleService,
                                       FileStorageService fileStorageService) {
        this.userAccountService = userAccountService;
        this.partnerService = partnerService;
        this.eventService = eventService;
        this.vehicleService = vehicleService;
        this.fileStorageService = fileStorageService;
    }

    @ModelAttribute("motivoOptions")
    public Motivo[] motivoOptions() {
        return Motivo.values();
    }

    @ModelAttribute("envolvimentoOptions")
    public Envolvimento[] envolvimentoOptions() {
        return Envolvimento.values();
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

                            // Busca todos os veículos do associado para o select de placas
                            java.util.List<Vehicle> myVehicles = vehicleService.listByPartnerId(partner.getId());
                            logger.info("Associado {} (ID: {}) possui {} veículos", partner.getName(), partner.getId(), myVehicles.size());

                            model.addAttribute("event", event);
                            model.addAttribute("partner", partner);
                            model.addAttribute("readOnlyPartner", true); // Flag para desabilitar seleção de associado
                            model.addAttribute("myEvents", myEvents); // Lista de eventos do associado
                            model.addAttribute("myVehicles", myVehicles); // Lista de veículos do associado

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
                                      @RequestParam(value = "docCrlv", required = false) MultipartFile docCrlv,
                                      @RequestParam(value = "docCnh", required = false) MultipartFile docCnh,
                                      @RequestParam(value = "docBo", required = false) MultipartFile docBo,
                                      @RequestParam(value = "docComprovanteResidencia", required = false) MultipartFile docComprovanteResidencia,
                                      @RequestParam(value = "docTermoAbertura", required = false) MultipartFile docTermoAbertura,
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

        // Força status padrão COMUNICADO (segurança - Associado não pode escolher status)
        event.setStatus(Status.COMUNICADO);

        if (result.hasErrors()) {
            model.addAttribute("partner", partner);
            model.addAttribute("readOnlyPartner", true);
            // Recarrega veículos e eventos para exibir novamente em caso de erro
            model.addAttribute("myVehicles", vehicleService.listByPartnerId(partner.getId()));
            model.addAttribute("myEvents", eventService.listByPartnerId(partner.getId()));
            return "cadastro_comunicado";
        }

        try {
            // Processa upload dos documentos
            if (docCrlv != null && !docCrlv.isEmpty()) {
                String crlvPath = fileStorageService.storeFile(docCrlv);
                event.setDocCrlvPath(crlvPath);
                logger.info("CRLV anexado ao comunicado: {}", crlvPath);
            }

            if (docCnh != null && !docCnh.isEmpty()) {
                String cnhPath = fileStorageService.storeFile(docCnh);
                event.setDocCnhPath(cnhPath);
                logger.info("CNH anexada ao comunicado: {}", cnhPath);
            }

            if (docBo != null && !docBo.isEmpty()) {
                String boPath = fileStorageService.storeFile(docBo);
                event.setDocBoPath(boPath);
                logger.info("B.O. anexado ao comunicado: {}", boPath);
            }

            if (docComprovanteResidencia != null && !docComprovanteResidencia.isEmpty()) {
                String compResPath = fileStorageService.storeFile(docComprovanteResidencia);
                event.setDocComprovanteResidenciaPath(compResPath);
                logger.info("Comprovante de residência anexado ao comunicado: {}", compResPath);
            }

            if (docTermoAbertura != null && !docTermoAbertura.isEmpty()) {
                String termoPath = fileStorageService.storeFile(docTermoAbertura);
                event.setDocTermoAberturaPath(termoPath);
                logger.info("Termo de abertura anexado ao comunicado: {}", termoPath);
            }

            eventService.create(event);
            logger.info("Comunicado criado com sucesso pelo associado: {} (ID: {})", partner.getName(), partner.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Comunicado cadastrado com sucesso! Aguarde o contato do administrador.");
            return "redirect:/me/comunicado";
        } catch (Exception ex) {
            logger.error("Erro ao criar comunicado para associado {}: ", partner.getName(), ex);
            model.addAttribute("formError", ex.getMessage() != null ? ex.getMessage() : "Não foi possível salvar o comunicado. Tente novamente.");
            model.addAttribute("partner", partner);
            model.addAttribute("readOnlyPartner", true);
            // Recarrega veículos e eventos para exibir novamente em caso de erro
            model.addAttribute("myVehicles", vehicleService.listByPartnerId(partner.getId()));
            model.addAttribute("myEvents", eventService.listByPartnerId(partner.getId()));
            return "cadastro_comunicado";
        }
    }

    /**
     * Atualiza a descrição de um comunicado do Associado
     */
    @PostMapping("/comunicado/update-description")
    public String updateDescription(@RequestParam Long eventId,
                                     @RequestParam String descricao,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            // Busca o associado do usuário logado
            var userOpt = userAccountService.findByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("formError", "Usuário não encontrado.");
                return "redirect:/me/comunicado";
            }

            var partnerOpt = partnerService.getPartnerByEmail(userOpt.get().getEmail());
            if (partnerOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("formError", "Seu usuário não está vinculado a um associado.");
                return "redirect:/me/comunicado";
            }

            Partner partner = partnerOpt.get();

            // Busca o evento
            var eventOpt = eventService.findById(eventId);
            if (eventOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("formError", "Evento não encontrado.");
                return "redirect:/me/comunicado";
            }

            Event event = eventOpt.get();

            // Valida que o evento pertence ao associado logado (segurança)
            if (event.getPartner() == null || !event.getPartner().getId().equals(partner.getId())) {
                logger.warn("Tentativa de edição não autorizada - Associado {} tentou editar evento {} de outro associado",
                        partner.getId(), eventId);
                redirectAttributes.addFlashAttribute("formError", "Você não tem permissão para editar este evento.");
                return "redirect:/me/comunicado";
            }

            // Atualiza a descrição usando updateWithHistory para rastrear mudanças
            event.setDescricao(descricao);
            String modifiedBy = partner.getName() + " (Associado)";
            eventService.updateWithHistory(eventId, event, modifiedBy);

            logger.info("Descrição do evento {} atualizada pelo associado {} (ID: {})", eventId, partner.getName(), partner.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Descrição atualizada com sucesso!");
            return "redirect:/me/comunicado";

        } catch (Exception ex) {
            logger.error("Erro ao atualizar descrição do evento {}: ", eventId, ex);
            redirectAttributes.addFlashAttribute("formError", "Não foi possível atualizar a descrição. Tente novamente.");
            return "redirect:/me/comunicado";
        }
    }

    /**
     * Atualiza as observações de um comunicado do Associado
     * As alterações são rastreadas no histórico
     */
    @PostMapping("/comunicado/update-observations")
    public String updateObservations(@RequestParam Long eventId,
                                      @RequestParam(required = false) String observacoes,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            // Busca o associado do usuário logado
            var userOpt = userAccountService.findByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("formError", "Usuário não encontrado.");
                return "redirect:/me/comunicado";
            }

            var partnerOpt = partnerService.getPartnerByEmail(userOpt.get().getEmail());
            if (partnerOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("formError", "Seu usuário não está vinculado a um associado.");
                return "redirect:/me/comunicado";
            }

            Partner partner = partnerOpt.get();

            // Busca o evento
            var eventOpt = eventService.findById(eventId);
            if (eventOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("formError", "Evento não encontrado.");
                return "redirect:/me/comunicado";
            }

            Event event = eventOpt.get();

            // Valida que o evento pertence ao associado logado (segurança)
            if (event.getPartner() == null || !event.getPartner().getId().equals(partner.getId())) {
                logger.warn("Tentativa de edição não autorizada - Associado {} tentou editar observações do evento {} de outro associado",
                        partner.getId(), eventId);
                redirectAttributes.addFlashAttribute("formError", "Você não tem permissão para editar este evento.");
                return "redirect:/me/comunicado";
            }

            // Atualiza as observações usando updateWithHistory para rastrear mudanças
            event.setObservacoes(observacoes);
            String modifiedBy = partner.getName() + " (Associado)";
            eventService.updateWithHistory(eventId, event, modifiedBy);

            logger.info("Observações do evento {} atualizadas pelo associado {} (ID: {})", eventId, partner.getName(), partner.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Observações atualizadas com sucesso!");
            return "redirect:/me/comunicado";

        } catch (Exception ex) {
            logger.error("Erro ao atualizar observações do evento {}: ", eventId, ex);
            redirectAttributes.addFlashAttribute("formError", "Não foi possível atualizar as observações. Tente novamente.");
            return "redirect:/me/comunicado";
        }
    }
}
