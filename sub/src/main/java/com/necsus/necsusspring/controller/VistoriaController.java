package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Status;
import com.necsus.necsusspring.model.Vistoria;
import com.necsus.necsusspring.model.VistoriaFoto;
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
                               @RequestParam(value = "pneuDianteiroDireito", required = false) MultipartFile pneuDianteiroDireito,
                               @RequestParam(value = "pneuDianteiroEsquerdo", required = false) MultipartFile pneuDianteiroEsquerdo,
                               @RequestParam(value = "pneuTraseiroDireito", required = false) MultipartFile pneuTraseiroDireito,
                               @RequestParam(value = "pneuTraseiroEsquerdo", required = false) MultipartFile pneuTraseiroEsquerdo,
                               @RequestParam(value = "fotos", required = false) MultipartFile[] fotos,
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
            logger.info("=== SALVANDO VISTORIA ===");
            logger.info("Event ID: {}, Vistoria ID: {}, Usuario: {}", eventId, vistoriaId, authentication.getName());
            logger.info("Fotos recebidas do campo geral: {}", fotos != null ? fotos.length : 0);
            logger.info("Observacoes recebidas: {}", observacoes != null ? "SIM" : "NAO");

            List<MultipartFile> arquivosRecebidos = new ArrayList<>();
            adicionarFotoSePresente(pneuDianteiroDireito, arquivosRecebidos, "Pneu dianteiro direito");
            adicionarFotoSePresente(pneuDianteiroEsquerdo, arquivosRecebidos, "Pneu dianteiro esquerdo");
            adicionarFotoSePresente(pneuTraseiroDireito, arquivosRecebidos, "Pneu traseiro direito");
            adicionarFotoSePresente(pneuTraseiroEsquerdo, arquivosRecebidos, "Pneu traseiro esquerdo");

            if (fotos != null && fotos.length > 0) {
                for (MultipartFile foto : fotos) {
                    if (foto != null && !foto.isEmpty()) {
                        arquivosRecebidos.add(foto);
                    }
                }
            }

            logger.info("Total de arquivos recebidos (pneus + gerais): {}", arquivosRecebidos.size());

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
                logger.info("Vistoria existente encontrada. ID: {}", vistoriaId);
                // Força inicialização da lista de fotos
                int fotosAnteriores = vistoria.getFotos().size();
                logger.info("Fotos já existentes na vistoria: {}", fotosAnteriores);
            } else {
                vistoria = new Vistoria();
                vistoria.setEvent(event);
                vistoria.setUsuarioCriacao(authentication.getName());
                logger.info("Nova vistoria criada");
            }

            vistoria.setObservacoes(observacoes);

            // Faz upload das fotos e cria as entidades VistoriaFoto
            // Cria uma lista separada para as fotos NOVAS
            List<VistoriaFoto> novasFotos = new ArrayList<>();
            int fotosAdicionadas = 0;
            if (!arquivosRecebidos.isEmpty()) {
                logger.info("Processando {} arquivo(s) de foto", arquivosRecebidos.size());

                // Calcula a ordem inicial baseada nas fotos existentes
                int ordem = vistoria.getFotos().size() + 1;

                for (MultipartFile foto : arquivosRecebidos) {
                    if (foto != null && !foto.isEmpty()) {
                        logger.info("Processando foto: {} (tamanho: {} bytes)", foto.getOriginalFilename(), foto.getSize());
                        String path = fileStorageService.storeFile(foto);
                        if (path != null) {
                            VistoriaFoto vistoriaFoto = new VistoriaFoto();
                            vistoriaFoto.setFotoPath(path);
                            vistoriaFoto.setOrdem(ordem++);
                            novasFotos.add(vistoriaFoto);
                            fotosAdicionadas++;
                            logger.info("Foto adicionada à lista temporária. Path: {}, Ordem: {}", path, vistoriaFoto.getOrdem());
                        } else {
                            logger.warn("Falha ao salvar arquivo: {}", foto.getOriginalFilename());
                        }
                    } else {
                        logger.warn("Foto nula ou vazia recebida");
                    }
                }
            } else {
                logger.warn("Nenhuma foto recebida no request");
            }

            logger.info("Total de fotos processadas: {}", fotosAdicionadas);

            // Salva a vistoria
            Vistoria vistoriaSalva;
            if (vistoriaId != null) {
                // Para update, passa apenas as fotos NOVAS
                vistoriaSalva = vistoriaService.updateWithNewPhotos(vistoriaId, vistoria.getObservacoes(), novasFotos);
                logger.info("Vistoria atualizada com sucesso. ID: {}, Evento ID: {}, Usuário: {}, Total fotos: {}",
                           vistoriaId, eventId, authentication.getName(), vistoriaSalva.getQuantidadeFotos());
            } else {
                // Para create, adiciona as fotos na vistoria
                novasFotos.forEach(vistoria::adicionarFoto);
                logger.info("Total de fotos na vistoria antes de salvar: {}", vistoria.getQuantidadeFotos());
                vistoriaSalva = vistoriaService.create(vistoria);
                logger.info("Vistoria criada com sucesso. ID: {}, Evento ID: {}, Usuário: {}, Total fotos: {}",
                           vistoriaSalva.getId(), eventId, authentication.getName(), vistoriaSalva.getQuantidadeFotos());
            }

            if (fotosAdicionadas > 0 && event.getStatus() != Status.ANALISE) {
                logger.info("Atualizando evento {} para o status ANALISE após envio de fotos", eventId);
                eventService.updateStatus(eventId, Status.ANALISE);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Fotos da vistoria enviadas com sucesso!");
            return "redirect:/vistorias";

        } catch (Exception ex) {
            logger.error("=== ERRO AO SALVAR VISTORIA ===");
            logger.error("Tipo da exceção: {}", ex.getClass().getName());
            logger.error("Mensagem: {}", ex.getMessage());
            logger.error("Stacktrace completo:", ex);

            String errorMsg = ex.getMessage() != null
                ? ex.getMessage()
                : "Erro interno: " + ex.getClass().getSimpleName();

            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao salvar vistoria: " + errorMsg);
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
            logger.info("Buscando vistorias para o evento ID: {}", eventId);
            List<Vistoria> vistorias = vistoriaService.listByEventId(eventId);
            logger.info("Encontradas {} vistorias para o evento {}", vistorias.size(), eventId);

            // Força o carregamento das fotos (que têm FetchType.LAZY) para que sejam serializadas
            vistorias.forEach(vistoria -> {
                int fotosCount = vistoria.getFotos().size(); // Força inicialização da coleção lazy
                logger.info("Vistoria ID {} tem {} fotos", vistoria.getId(), fotosCount);
            });

            return ResponseEntity.ok(vistorias);
        } catch (Exception ex) {
            logger.error("Erro ao buscar vistorias do evento {}: ", eventId, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download de foto da vistoria
     */
    @GetMapping("/{vistoriaId}/download/{fotoId}")
    public ResponseEntity<Resource> downloadFoto(@PathVariable Long vistoriaId,
                                                  @PathVariable Long fotoId) {
        try {
            Vistoria vistoria = vistoriaService.findById(vistoriaId)
                    .orElseThrow(() -> new RuntimeException("Vistoria não encontrada"));

            VistoriaFoto foto = vistoria.getFotos().stream()
                    .filter(f -> f.getId().equals(fotoId))
                    .findFirst()
                    .orElse(null);

            if (foto == null) {
                return ResponseEntity.notFound().build();
            }

            String filePath = foto.getFotoPath();

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

    private void adicionarFotoSePresente(MultipartFile arquivo, List<MultipartFile> destino, String origem) {
        if (arquivo != null && !arquivo.isEmpty()) {
            logger.info("Foto recebida do campo {}: {} ({} bytes)", origem, arquivo.getOriginalFilename(), arquivo.getSize());
            destino.add(arquivo);
        } else {
            logger.info("Nenhum arquivo enviado para o campo {}", origem);
        }
    }
}
