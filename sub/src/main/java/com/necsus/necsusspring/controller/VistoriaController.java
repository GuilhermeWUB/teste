package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Status;
import com.necsus.necsusspring.model.Vistoria;
import com.necsus.necsusspring.service.EventService;
import com.necsus.necsusspring.service.FileStorageService;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.UserAccountService;
import com.necsus.necsusspring.service.VistoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/vistorias")
public class VistoriaController {

    private static final Logger logger = LoggerFactory.getLogger(VistoriaController.class);

    private final VistoriaService vistoriaService;
    private final EventService eventService;
    private final PartnerService partnerService;
    private final UserAccountService userAccountService;
    private final FileStorageService fileStorageService;

    public VistoriaController(VistoriaService vistoriaService,
                              EventService eventService,
                              PartnerService partnerService,
                              UserAccountService userAccountService,
                              FileStorageService fileStorageService) {
        this.vistoriaService = vistoriaService;
        this.eventService = eventService;
        this.partnerService = partnerService;
        this.userAccountService = userAccountService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Lista os comunicados do associado que estão no status VISTORIA
     */
    @GetMapping
    public String index(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String userRole = getUserRole(authentication);

        // Bloqueia acesso de usuários admin - apenas usuários Associado (USER) podem acessar
        if (!"USER".equals(userRole)) {
            logger.warn("Acesso negado às vistorias. Apenas role USER/Associado tem acesso. Role atual: {}", userRole);
            return "redirect:/";
        }

        // Busca o parceiro associado ao usuário logado
        Partner partner = getPartnerForUser(authentication);

        if (partner == null) {
            logger.warn("Associado não encontrado para o usuário: {}", authentication.getName());
            return "redirect:/comunicados";
        }

        // Busca os eventos do associado que estão no status VISTORIA
        List<Event> userEvents = eventService.listByPartnerId(partner.getId());
        List<Event> eventosVistoria = userEvents.stream()
                .filter(event -> event.getStatus() == Status.VISTORIA)
                .toList();

        logger.info("Encontrados {} eventos no status VISTORIA para o associado ID: {}", eventosVistoria.size(), partner.getId());

        model.addAttribute("pageTitle", "SUB - Vistorias");
        model.addAttribute("userPartner", partner);
        model.addAttribute("eventosVistoria", eventosVistoria);

        return "vistorias";
    }

    /**
     * Exibe o formulário de vistoria para um evento específico
     */
    @GetMapping("/{eventId}/form")
    public String showForm(@PathVariable Long eventId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) {
            logger.warn("Acesso negado ao formulário de vistoria. Apenas role USER/Associado tem acesso. Role atual: {}", userRole);
            return "redirect:/";
        }

        // Busca o evento
        Optional<Event> eventOpt = eventService.findById(eventId);
        if (eventOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Evento não encontrado.");
            return "redirect:/vistorias";
        }

        Event event = eventOpt.get();

        // Valida se o evento pertence ao associado logado
        Partner partner = getPartnerForUser(authentication);
        if (partner == null || !event.getPartner().getId().equals(partner.getId())) {
            logger.warn("Associado {} tentou acessar vistoria do evento {} que não pertence a ele",
                       authentication.getName(), eventId);
            redirectAttributes.addFlashAttribute("errorMessage", "Você não tem permissão para acessar esta vistoria.");
            return "redirect:/vistorias";
        }

        // Valida se o evento está no status VISTORIA
        if (event.getStatus() != Status.VISTORIA) {
            redirectAttributes.addFlashAttribute("errorMessage", "Este evento não está no status de Vistoria.");
            return "redirect:/vistorias";
        }

        // Busca vistoria existente ou cria uma nova
        Optional<Vistoria> vistoriaOpt = vistoriaService.findLatestByEventId(eventId);
        Vistoria vistoria;
        if (vistoriaOpt.isPresent()) {
            vistoria = vistoriaOpt.get();
        } else {
            vistoria = new Vistoria();
            vistoria.setEvent(event);
        }

        model.addAttribute("pageTitle", "SUB - Vistoria do Acidente");
        model.addAttribute("event", event);
        model.addAttribute("vistoria", vistoria);
        model.addAttribute("userPartner", partner);

        return "vistoria_form";
    }

    /**
     * Salva as fotos da vistoria
     */
    @PostMapping("/salvar")
    public String saveVistoria(@RequestParam("eventId") Long eventId,
                               @RequestParam(value = "vistoriaId", required = false) Long vistoriaId,
                               @RequestParam(value = "observacoes", required = false) String observacoes,
                               @RequestParam(value = "foto1", required = false) MultipartFile foto1,
                               @RequestParam(value = "foto2", required = false) MultipartFile foto2,
                               @RequestParam(value = "foto3", required = false) MultipartFile foto3,
                               @RequestParam(value = "foto4", required = false) MultipartFile foto4,
                               @RequestParam(value = "foto5", required = false) MultipartFile foto5,
                               @RequestParam(value = "foto6", required = false) MultipartFile foto6,
                               @RequestParam(value = "foto7", required = false) MultipartFile foto7,
                               @RequestParam(value = "foto8", required = false) MultipartFile foto8,
                               @RequestParam(value = "foto9", required = false) MultipartFile foto9,
                               @RequestParam(value = "foto10", required = false) MultipartFile foto10,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        if (authentication == null) {
            return "redirect:/login";
        }

        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) {
            logger.warn("Acesso negado para salvar vistoria. Apenas role USER/Associado tem acesso. Role atual: {}", userRole);
            return "redirect:/";
        }

        try {
            // Busca o evento
            Event event = eventService.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado"));

            // Valida se o evento pertence ao associado logado
            Partner partner = getPartnerForUser(authentication);
            if (partner == null || !event.getPartner().getId().equals(partner.getId())) {
                logger.warn("Associado {} tentou salvar vistoria do evento {} que não pertence a ele",
                           authentication.getName(), eventId);
                redirectAttributes.addFlashAttribute("errorMessage", "Você não tem permissão para editar esta vistoria.");
                return "redirect:/vistorias";
            }

            // Busca vistoria existente ou cria uma nova
            Vistoria vistoria;
            if (vistoriaId != null) {
                vistoria = vistoriaService.findById(vistoriaId)
                        .orElseThrow(() -> new IllegalArgumentException("Vistoria não encontrada"));
            } else {
                vistoria = new Vistoria();
                vistoria.setEvent(event);
                vistoria.setUsuarioCriacao(authentication.getName());
            }

            // Faz upload das fotos e armazena os paths
            if (foto1 != null && !foto1.isEmpty()) {
                String path = fileStorageService.storeFile(foto1);
                if (path != null) vistoria.setFoto1Path(path);
            }
            if (foto2 != null && !foto2.isEmpty()) {
                String path = fileStorageService.storeFile(foto2);
                if (path != null) vistoria.setFoto2Path(path);
            }
            if (foto3 != null && !foto3.isEmpty()) {
                String path = fileStorageService.storeFile(foto3);
                if (path != null) vistoria.setFoto3Path(path);
            }
            if (foto4 != null && !foto4.isEmpty()) {
                String path = fileStorageService.storeFile(foto4);
                if (path != null) vistoria.setFoto4Path(path);
            }
            if (foto5 != null && !foto5.isEmpty()) {
                String path = fileStorageService.storeFile(foto5);
                if (path != null) vistoria.setFoto5Path(path);
            }
            if (foto6 != null && !foto6.isEmpty()) {
                String path = fileStorageService.storeFile(foto6);
                if (path != null) vistoria.setFoto6Path(path);
            }
            if (foto7 != null && !foto7.isEmpty()) {
                String path = fileStorageService.storeFile(foto7);
                if (path != null) vistoria.setFoto7Path(path);
            }
            if (foto8 != null && !foto8.isEmpty()) {
                String path = fileStorageService.storeFile(foto8);
                if (path != null) vistoria.setFoto8Path(path);
            }
            if (foto9 != null && !foto9.isEmpty()) {
                String path = fileStorageService.storeFile(foto9);
                if (path != null) vistoria.setFoto9Path(path);
            }
            if (foto10 != null && !foto10.isEmpty()) {
                String path = fileStorageService.storeFile(foto10);
                if (path != null) vistoria.setFoto10Path(path);
            }

            vistoria.setObservacoes(observacoes);

            // Salva a vistoria
            if (vistoriaId != null) {
                vistoriaService.update(vistoriaId, vistoria);
                logger.info("Vistoria atualizada com sucesso. ID: {}, Evento ID: {}, Usuário: {}",
                           vistoriaId, eventId, authentication.getName());
            } else {
                vistoriaService.create(vistoria);
                logger.info("Vistoria criada com sucesso. Evento ID: {}, Usuário: {}",
                           eventId, authentication.getName());
            }

            redirectAttributes.addFlashAttribute("successMessage", "Fotos da vistoria enviadas com sucesso!");
            return "redirect:/vistorias";

        } catch (Exception ex) {
            logger.error("Erro ao salvar vistoria: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao salvar vistoria: " + ex.getMessage());
            return "redirect:/vistorias/" + eventId + "/form";
        }
    }

    /**
     * API para buscar vistorias de um evento (usado pelo Kanban)
     */
    @GetMapping("/api/event/{eventId}")
    @ResponseBody
    public ResponseEntity<List<Vistoria>> getVistoriasByEventId(@PathVariable Long eventId) {
        try {
            List<Vistoria> vistorias = vistoriaService.listByEventId(eventId);
            return ResponseEntity.ok(vistorias);
        } catch (Exception ex) {
            logger.error("Erro ao buscar vistorias do evento {}: ", eventId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download de foto da vistoria
     */
    @GetMapping("/{vistoriaId}/download/{fotoNumero}")
    public ResponseEntity<Resource> downloadFoto(@PathVariable Long vistoriaId,
                                                  @PathVariable int fotoNumero) {
        try {
            Vistoria vistoria = vistoriaService.findById(vistoriaId)
                    .orElseThrow(() -> new RuntimeException("Vistoria não encontrada"));

            String filePath = switch (fotoNumero) {
                case 1 -> vistoria.getFoto1Path();
                case 2 -> vistoria.getFoto2Path();
                case 3 -> vistoria.getFoto3Path();
                case 4 -> vistoria.getFoto4Path();
                case 5 -> vistoria.getFoto5Path();
                case 6 -> vistoria.getFoto6Path();
                case 7 -> vistoria.getFoto7Path();
                case 8 -> vistoria.getFoto8Path();
                case 9 -> vistoria.getFoto9Path();
                case 10 -> vistoria.getFoto10Path();
                default -> null;
            };

            if (filePath == null || filePath.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            String filename = path.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception ex) {
            logger.error("Erro ao fazer download da foto: ", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .findFirst()
                .orElse("USER");
    }

    private Partner getPartnerForUser(Authentication authentication) {
        return userAccountService.findByUsername(authentication.getName())
                .flatMap(user -> partnerService.getPartnerByEmail(user.getEmail()))
                .orElse(null);
    }
}
