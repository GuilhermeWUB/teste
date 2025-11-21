package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Status;
import com.necsus.necsusspring.model.Vistoria;
import com.necsus.necsusspring.model.VistoriaFoto;
import com.necsus.necsusspring.service.EventService;
import com.necsus.necsusspring.service.FileStorageService;
import com.necsus.necsusspring.service.GeminiService;
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
import java.util.ArrayList;
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
    private final GeminiService geminiService;

    public VistoriaController(VistoriaService vistoriaService,
                              EventService eventService,
                              PartnerService partnerService,
                              UserAccountService userAccountService,
                              FileStorageService fileStorageService,
                              GeminiService geminiService) {
        this.vistoriaService = vistoriaService;
        this.eventService = eventService;
        this.partnerService = partnerService;
        this.userAccountService = userAccountService;
        this.fileStorageService = fileStorageService;
        this.geminiService = geminiService;
    }

    // ... (Métodos index e showForm permanecem iguais) ...
    @GetMapping
    public String index(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) return "redirect:/";
        Partner partner = getPartnerForUser(authentication);
        if (partner == null) return "redirect:/comunicados";
        List<Event> userEvents = eventService.listByPartnerId(partner.getId());
        List<Event> eventosVistoria = userEvents.stream().filter(event -> event.getStatus() == Status.VISTORIA).toList();
        model.addAttribute("pageTitle", "SUB - Vistorias");
        model.addAttribute("userPartner", partner);
        model.addAttribute("eventosVistoria", eventosVistoria);
        return "vistorias";
    }

    @GetMapping("/{eventId}/form")
    public String showForm(@PathVariable Long eventId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";
        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) return "redirect:/";
        Optional<Event> eventOpt = eventService.findById(eventId);
        if (eventOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Evento não encontrado.");
            return "redirect:/vistorias";
        }
        Event event = eventOpt.get();
        Partner partner = getPartnerForUser(authentication);
        if (partner == null || !event.getPartner().getId().equals(partner.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Você não tem permissão.");
            return "redirect:/vistorias";
        }
        if (event.getStatus() != Status.VISTORIA) {
            redirectAttributes.addFlashAttribute("errorMessage", "Este evento não está no status de Vistoria.");
            return "redirect:/vistorias";
        }
        Optional<Vistoria> vistoriaOpt = vistoriaService.findLatestByEventId(eventId);
        Vistoria vistoria = vistoriaOpt.orElseGet(() -> {
            Vistoria v = new Vistoria();
            v.setEvent(event);
            return v;
        });
        model.addAttribute("pageTitle", "SUB - Vistoria do Acidente");
        model.addAttribute("event", event);
        model.addAttribute("vistoria", vistoria);
        model.addAttribute("userPartner", partner);
        return "vistoria_form";
    }

    @PostMapping("/salvar")
    public String saveVistoria(@RequestParam("eventId") Long eventId,
                               @RequestParam(value = "vistoriaId", required = false) Long vistoriaId,
                               @RequestParam(value = "observacoes", required = false) String observacoes,
                               @RequestParam(value = "pneuDianteiroDireito", required = false) MultipartFile pneuDianteiroDireito,
                               @RequestParam(value = "pneuDianteiroEsquerdo", required = false) MultipartFile pneuDianteiroEsquerdo,
                               @RequestParam(value = "pneuTraseiroDireito", required = false) MultipartFile pneuTraseiroDireito,
                               @RequestParam(value = "pneuTraseiroEsquerdo", required = false) MultipartFile pneuTraseiroEsquerdo,
                               @RequestParam(value = "fotos", required = false) MultipartFile[] fotos,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        if (authentication == null) return "redirect:/login";
        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) return "redirect:/";

        try {
            List<MultipartFile> arquivosRecebidos = new ArrayList<>();
            adicionarFotoSePresente(pneuDianteiroDireito, arquivosRecebidos, "Pneu DD");
            adicionarFotoSePresente(pneuDianteiroEsquerdo, arquivosRecebidos, "Pneu DE");
            adicionarFotoSePresente(pneuTraseiroDireito, arquivosRecebidos, "Pneu TD");
            adicionarFotoSePresente(pneuTraseiroEsquerdo, arquivosRecebidos, "Pneu TE");

            if (fotos != null && fotos.length > 0) {
                for (MultipartFile foto : fotos) {
                    if (foto != null && !foto.isEmpty()) arquivosRecebidos.add(foto);
                }
            }

            Event event = eventService.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado"));

            Partner partner = getPartnerForUser(authentication);
            if (partner == null || !event.getPartner().getId().equals(partner.getId())) {
                return "redirect:/vistorias";
            }

            Vistoria vistoria;
            if (vistoriaId != null) {
                vistoria = vistoriaService.findById(vistoriaId)
                        .orElseThrow(() -> new IllegalArgumentException("Vistoria não encontrada"));
            } else {
                vistoria = new Vistoria();
                vistoria.setEvent(event);
                vistoria.setUsuarioCriacao(authentication.getName());
            }

            // Garante que não estamos re-salvando fotos antigas aqui
            // O método updateWithNewPhotos já lida com a adição segura

            List<VistoriaFoto> novasFotos = new ArrayList<>();
            int fotosAdicionadas = 0;
            if (!arquivosRecebidos.isEmpty()) {
                int ordem = vistoria.getFotos().size() + 1;
                for (MultipartFile foto : arquivosRecebidos) {
                    String path = fileStorageService.storeFile(foto);
                    if (path != null) {
                        VistoriaFoto vistoriaFoto = new VistoriaFoto();
                        vistoriaFoto.setFotoPath(path);
                        vistoriaFoto.setOrdem(ordem++);
                        novasFotos.add(vistoriaFoto);
                        fotosAdicionadas++;
                    }
                }
            }

            // Salva a vistoria e as fotos no banco primeiro
            Vistoria vistoriaSalva;
            if (vistoriaId != null) {
                vistoriaSalva = vistoriaService.updateWithNewPhotos(vistoriaId, observacoes, novasFotos);
            } else {
                vistoria.setObservacoes(observacoes);
                novasFotos.forEach(vistoria::adicionarFoto);
                vistoriaSalva = vistoriaService.create(vistoria);
            }

            // === INTEGRAÇÃO COM GEMINI AI ===
            if (!arquivosRecebidos.isEmpty()) {
                logger.info("Chamando Gemini para analisar {} novas fotos...", arquivosRecebidos.size());
                try {
                    String contextoEvento = "Descrição do Evento: " + event.getDescricao() +
                            ". Observações: " + event.getObservacoes();

                    String analise = geminiService.analisarVistoria(contextoEvento, arquivosRecebidos);

                    // FIX: Usa o método específico para atualizar só o texto, evitando erro de concorrência
                    vistoriaService.updateAnaliseIa(vistoriaSalva.getId(), analise);

                } catch (Exception e) {
                    logger.error("Falha não obstativa na análise IA: ", e);
                }
            }
            // ================================

            if (fotosAdicionadas > 0 && event.getStatus() != Status.ANALISE) {
                eventService.updateStatus(eventId, Status.ANALISE);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Fotos enviadas e análise solicitada!");
            return "redirect:/vistorias";

        } catch (Exception ex) {
            logger.error("Erro ao salvar vistoria", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao salvar: " + ex.getMessage());
            return "redirect:/vistorias/" + eventId + "/form";
        }
    }

    // ... (Resto dos métodos iguais) ...

    @GetMapping("/api/event/{eventId}")
    @ResponseBody
    public ResponseEntity<List<Vistoria>> getVistoriasByEventId(@PathVariable Long eventId) {
        try {
            List<Vistoria> vistorias = vistoriaService.listByEventId(eventId);
            vistorias.forEach(vistoria -> {
                if(vistoria.getFotos() != null) vistoria.getFotos().size();
            });
            return ResponseEntity.ok(vistorias);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{vistoriaId}/download/{fotoId}")
    public ResponseEntity<Resource> downloadFoto(@PathVariable Long vistoriaId, @PathVariable Long fotoId) {
        try {
            Vistoria vistoria = vistoriaService.findById(vistoriaId)
                    .orElseThrow(() -> new RuntimeException("Vistoria não encontrada"));
            VistoriaFoto foto = vistoria.getFotos().stream()
                    .filter(f -> f.getId().equals(fotoId))
                    .findFirst().orElse(null);
            if (foto == null) return ResponseEntity.notFound().build();
            Path path = Paths.get(foto.getFotoPath());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .findFirst().orElse("USER");
    }

    private Partner getPartnerForUser(Authentication authentication) {
        return userAccountService.findByUsername(authentication.getName())
                .flatMap(user -> partnerService.getPartnerByEmail(user.getEmail()))
                .orElse(null);
    }

    private void adicionarFotoSePresente(MultipartFile arquivo, List<MultipartFile> destino, String origem) {
        if (arquivo != null && !arquivo.isEmpty()) destino.add(arquivo);
    }
}