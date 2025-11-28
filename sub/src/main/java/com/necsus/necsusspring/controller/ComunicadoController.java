package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.ComunicadoService;
import com.necsus.necsusspring.service.EventService;
import com.necsus.necsusspring.service.FileStorageService;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.UserAccountService;
import com.necsus.necsusspring.service.VistoriaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/comunicados")
public class ComunicadoController {

    private static final Logger logger = LoggerFactory.getLogger(ComunicadoController.class);

    private final ComunicadoService comunicadoService;
    private final EventService eventService;
    private final PartnerService partnerService;
    private final UserAccountService userAccountService;
    private final VistoriaService vistoriaService;
    private final FileStorageService fileStorageService;

    public ComunicadoController(ComunicadoService comunicadoService,
                                EventService eventService,
                                PartnerService partnerService,
                                UserAccountService userAccountService,
                                VistoriaService vistoriaService,
                                FileStorageService fileStorageService) {
        this.comunicadoService = comunicadoService;
        this.eventService = eventService;
        this.partnerService = partnerService;
        this.userAccountService = userAccountService;
        this.vistoriaService = vistoriaService;
        this.fileStorageService = fileStorageService;
    }

    @ModelAttribute("motivoOptions")
    public Motivo[] motivoOptions() {
        return Motivo.values();
    }

    @GetMapping("/api/events/{eventId}")
    @ResponseBody
    public ResponseEntity<?> getEventDetails(@PathVariable Long eventId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sua sessão expirou. Faça login novamente."));
        }

        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) {
            logger.warn("[COMUNICADOS] Acesso negado à API de detalhes para role {}", userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Apenas associados podem consultar o comunicado."));
        }

        Partner partner = getPartnerForUser(authentication);
        if (partner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Não foi possível localizar seus dados de associado."));
        }

        Optional<Event> eventOpt = eventService.findById(eventId);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Comunicado não encontrado."));
        }

        Event event = eventOpt.get();
        if (event.getPartner() == null || !partner.getId().equals(event.getPartner().getId())) {
            logger.warn("[COMUNICADOS] Associado {} tentou acessar comunicado {} que não é seu", partner.getId(), eventId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para acessar este comunicado."));
        }

        return ResponseEntity.ok(buildEventDetailsPayload(event));
    }

    @PostMapping(value = "/api/events/{eventId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<?> updateEventDetails(@PathVariable Long eventId,
                                                @RequestParam(required = false) String descricao,
                                                @RequestParam(required = false) String observacoes,
                                                @RequestParam(value = "novasFotos", required = false) MultipartFile[] novasFotos,
                                                Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sua sessão expirou. Faça login novamente."));
        }

        String userRole = getUserRole(authentication);
        if (!"USER".equals(userRole)) {
            logger.warn("[COMUNICADOS] Acesso negado à API de atualização para role {}", userRole);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Apenas associados podem editar o comunicado."));
        }

        Partner partner = getPartnerForUser(authentication);
        if (partner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Não foi possível localizar seus dados de associado."));
        }

        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado"));

        if (event.getPartner() == null || !partner.getId().equals(event.getPartner().getId())) {
            logger.warn("[COMUNICADOS] Associado {} tentou editar comunicado {} que não é seu", partner.getId(), eventId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para editar este comunicado."));
        }

        String modifiedBy = partner.getName() + " (Associado)";
        Map<String, Object> updates = new HashMap<>();
        if (descricao != null) {
            updates.put("descricao", descricao);
        }
        if (observacoes != null) {
            updates.put("observacoes", observacoes);
        }

        if (!updates.isEmpty()) {
            eventService.updatePartialWithHistory(eventId, updates, modifiedBy);
            logger.info("[COMUNICADOS] Associado {} atualizou campos {} do comunicado {}",
                    partner.getId(), updates.keySet(), eventId);
        }

        boolean fotosEnviadas = anexarNovasFotos(event, novasFotos, authentication);
        if (fotosEnviadas) {
            logger.info("[COMUNICADOS] {} anexou novas fotos ao comunicado {}", authentication.getName(), eventId);
        }

        Event updated = eventService.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado"));

        Map<String, Object> payload = buildEventDetailsPayload(updated);
        payload.put("success", true);
        payload.put("message", fotosEnviadas ?
                "Dados atualizados e fotos enviadas com sucesso!" : "Dados atualizados com sucesso!");

        return ResponseEntity.ok(payload);
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
        event.setStatus(Status.COMUNICADO); // Status padrão para associados
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
                              Model model,
                              @RequestParam(value = "docCrlv", required = false) MultipartFile docCrlv,
                              @RequestParam(value = "docCnh", required = false) MultipartFile docCnh,
                              @RequestParam(value = "docBo", required = false) MultipartFile docBo,
                              @RequestParam(value = "docComprovanteResidencia", required = false) MultipartFile docComprovanteResidencia,
                              @RequestParam(value = "docTermoAbertura", required = false) MultipartFile docTermoAbertura,
                              @RequestParam(value = "fotosAcidente", required = false) MultipartFile[] fotosAcidente,
                              @RequestParam(value = "terceiroEnvolvido", required = false) Boolean terceiroEnvolvido,
                              @RequestParam(value = "terceiroNome", required = false) String terceiroNome,
                              @RequestParam(value = "terceiroCpf", required = false) String terceiroCpf,
                              @RequestParam(value = "terceiroTelefone", required = false) String terceiroTelefone,
                              @RequestParam(value = "docTerceiroCnh", required = false) MultipartFile docTerceiroCnh,
                              @RequestParam(value = "docTerceiroCrlv", required = false) MultipartFile docTerceiroCrlv,
                              @RequestParam(value = "docTerceiroOutros", required = false) MultipartFile docTerceiroOutros) {

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

        // Define o parceiro e o status automaticamente como o usuário logado
        event.setPartner(partner);
        event.setStatus(Status.COMUNICADO);

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
            // Define se há terceiro envolvido
            event.setTerceiroEnvolvido(terceiroEnvolvido != null && terceiroEnvolvido);

            // Define informações pessoais do terceiro (se houver)
            if (Boolean.TRUE.equals(terceiroEnvolvido)) {
                event.setTerceiroNome(terceiroNome);
                event.setTerceiroCpf(terceiroCpf);
                event.setTerceiroTelefone(terceiroTelefone);
            }

            // Anexa documentos do associado
            anexarDocumentos(event, docCrlv, docCnh, docBo, docComprovanteResidencia, docTermoAbertura);

            // Anexa documentos de terceiros (se houver)
            if (Boolean.TRUE.equals(terceiroEnvolvido)) {
                anexarDocumentosTerceiro(event, docTerceiroCnh, docTerceiroCrlv, docTerceiroOutros);
            }

            // Cria o evento
            Event eventoSalvo = eventService.create(event);
            logger.info("Evento criado com sucesso pelo usuário Associado: {}", authentication.getName());

            // Cria vistoria com fotos do acidente (se houver)
            if (fotosAcidente != null && fotosAcidente.length > 0) {
                boolean fotosSalvas = anexarFotosAcidente(eventoSalvo, fotosAcidente, authentication);
                if (fotosSalvas) {
                    logger.info("Fotos do acidente anexadas ao evento {} pelo usuário {}", eventoSalvo.getId(), authentication.getName());
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Evento cadastrado com sucesso! Nossa equipe entrará em contato em breve.");
            return "redirect:/comunicados";
        } catch (Exception ex) {
            logger.error("Erro ao criar evento pelo usuário Associado: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Não foi possível salvar o evento. Tente novamente.");
            return "redirect:/comunicados";
        }
    }

    private void anexarDocumentos(Event event,
                                   MultipartFile docCrlv,
                                   MultipartFile docCnh,
                                   MultipartFile docBo,
                                   MultipartFile docComprovanteResidencia,
                                   MultipartFile docTermoAbertura) {
        if (docCrlv != null && !docCrlv.isEmpty()) {
            String path = fileStorageService.storeFile(docCrlv);
            event.setDocCrlvPath(path);
            logger.info("[COMUNICADOS] CRLV anexado: {}", path);
        }

        if (docCnh != null && !docCnh.isEmpty()) {
            String path = fileStorageService.storeFile(docCnh);
            event.setDocCnhPath(path);
            logger.info("[COMUNICADOS] CNH anexada: {}", path);
        }

        if (docBo != null && !docBo.isEmpty()) {
            String path = fileStorageService.storeFile(docBo);
            event.setDocBoPath(path);
            logger.info("[COMUNICADOS] B.O. anexado: {}", path);
        }

        if (docComprovanteResidencia != null && !docComprovanteResidencia.isEmpty()) {
            String path = fileStorageService.storeFile(docComprovanteResidencia);
            event.setDocComprovanteResidenciaPath(path);
            logger.info("[COMUNICADOS] Comprovante de residência anexado: {}", path);
        }

        if (docTermoAbertura != null && !docTermoAbertura.isEmpty()) {
            String path = fileStorageService.storeFile(docTermoAbertura);
            event.setDocTermoAberturaPath(path);
            logger.info("[COMUNICADOS] Termo de abertura anexado: {}", path);
        }
    }

    private void anexarDocumentosTerceiro(Event event,
                                          MultipartFile docTerceiroCnh,
                                          MultipartFile docTerceiroCrlv,
                                          MultipartFile docTerceiroOutros) {
        if (docTerceiroCnh != null && !docTerceiroCnh.isEmpty()) {
            String path = fileStorageService.storeFile(docTerceiroCnh);
            event.setDocTerceiroCnhPath(path);
            logger.info("[COMUNICADOS] CNH do terceiro anexada: {}", path);
        }

        if (docTerceiroCrlv != null && !docTerceiroCrlv.isEmpty()) {
            String path = fileStorageService.storeFile(docTerceiroCrlv);
            event.setDocTerceiroCrlvPath(path);
            logger.info("[COMUNICADOS] CRLV do terceiro anexado: {}", path);
        }

        if (docTerceiroOutros != null && !docTerceiroOutros.isEmpty()) {
            String path = fileStorageService.storeFile(docTerceiroOutros);
            event.setDocTerceiroOutrosPath(path);
            logger.info("[COMUNICADOS] Outros documentos do terceiro anexados: {}", path);
        }
    }

    private boolean anexarFotosAcidente(Event event, MultipartFile[] fotosAcidente, Authentication authentication) {
        if (fotosAcidente == null || fotosAcidente.length == 0) {
            return false;
        }

        List<VistoriaFoto> fotos = new ArrayList<>();
        for (MultipartFile arquivo : fotosAcidente) {
            if (arquivo == null || arquivo.isEmpty()) {
                continue;
            }
            String path = fileStorageService.storeFile(arquivo);
            if (path == null) {
                continue;
            }
            VistoriaFoto foto = new VistoriaFoto();
            foto.setFotoPath(path.replace("\\", "/"));
            fotos.add(foto);
        }

        if (fotos.isEmpty()) {
            return false;
        }

        // Cria uma nova vistoria para este evento
        Vistoria vistoria = new Vistoria();
        vistoria.setEvent(event);
        vistoria.setUsuarioCriacao(authentication != null ? authentication.getName() : "Sistema");
        vistoria.setObservacoes("Fotos do acidente enviadas pelo associado no momento da criação do comunicado");
        vistoria = vistoriaService.create(vistoria);

        // Adiciona as fotos à vistoria com ordenação
        int ordem = 0;
        for (VistoriaFoto foto : fotos) {
            foto.setOrdem(++ordem);
            foto.setVistoria(vistoria);
        }

        vistoriaService.updateWithNewPhotos(vistoria.getId(), vistoria.getObservacoes(), fotos);
        return true;
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

    private Map<String, Object> buildEventDetailsPayload(Event event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", event.getId());
        payload.put("titulo", event.getTitulo());
        payload.put("descricao", event.getDescricao());
        payload.put("descricaoResumo", resumir(event.getDescricao()));
        payload.put("observacoes", event.getObservacoes());
        payload.put("status", event.getStatus() != null ? event.getStatus().name() : null);
        payload.put("statusFormatted", event.getStatus() != null ? event.getStatus().getDisplayName() : null);
        payload.put("statusBadgeClass", resolveStatusBadgeClass(event.getStatus()));
        payload.put("statusPhase", event.getStatus() != null ? event.getStatus().getPhase() : null);
        payload.put("statusPhaseName", event.getStatus() != null ? event.getStatus().getPhaseName() : null);
        payload.put("motivo", event.getMotivo() != null ? event.getMotivo().name() : null);
        payload.put("motivoDescricao", event.getMotivo() != null ? event.getMotivo().getDescricao() : null);
        payload.put("envolvimento", event.getEnvolvimento() != null ? event.getEnvolvimento().name() : null);
        payload.put("envolvimentoDescricao", event.getEnvolvimento() != null ? event.getEnvolvimento().getDescricao() : null);
        payload.put("partnerName", event.getPartner() != null ? event.getPartner().getName() : null);
        payload.put("vehiclePlaque", event.getVehicle() != null && event.getVehicle().getPlaque() != null
                ? event.getVehicle().getPlaque()
                : event.getPlacaManual());
        payload.put("vehicleModel", event.getVehicle() != null ? event.getVehicle().getModel() : null);
        payload.put("vehicleMaker", event.getVehicle() != null ? event.getVehicle().getMaker() : null);
        payload.put("dataAconteceu", formatDate(event.getDataAconteceu()));
        payload.put("horaAconteceu", formatHora(event.getHoraAconteceu()));
        payload.put("dataComunicacao", formatDate(event.getDataComunicacao()));
        payload.put("horaComunicacao", formatHora(event.getHoraComunicacao()));
        payload.put("attachments", buildDocumentList(event));
        payload.put("terceiroEnvolvido", event.getTerceiroEnvolvido());
        payload.put("terceiroNome", event.getTerceiroNome());
        payload.put("terceiroCpf", event.getTerceiroCpf());
        payload.put("terceiroTelefone", event.getTerceiroTelefone());

        Optional<Vistoria> vistoriaOpt = vistoriaService.findLatestWithPhotosByEventId(event.getId());
        if (vistoriaOpt.isPresent()) {
            Vistoria vistoria = vistoriaOpt.get();
            List<Map<String, Object>> fotos = vistoria.getFotos().stream()
                    .sorted(java.util.Comparator.comparing(VistoriaFoto::getOrdem, java.util.Comparator.nullsLast(Integer::compareTo)))
                    .map(this::toFotoPayload)
                    .toList();
            payload.put("vistoriaId", vistoria.getId());
            payload.put("vistoriaObservacoes", vistoria.getObservacoes());
            payload.put("photos", fotos);
            payload.put("photoCount", fotos.size());
        } else {
            payload.put("photos", List.of());
            payload.put("photoCount", 0);
        }

        return payload;
    }

    private Map<String, Object> toFotoPayload(VistoriaFoto foto) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", foto.getId());
        map.put("ordem", foto.getOrdem());
        map.put("url", toPublicUrl(foto.getFotoPath()));
        map.put("path", foto.getFotoPath());
        map.put("uploadedAt", foto.getDataCriacao() != null ? foto.getDataCriacao().toString() : null);
        return map;
    }

    private String toPublicUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        String normalized = storedPath.replace("\\", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private List<Map<String, String>> buildDocumentList(Event event) {
        List<Map<String, String>> docs = new ArrayList<>();
        addDocIfPresent(docs, "CRLV", event.getDocCrlvPath());
        addDocIfPresent(docs, "CNH", event.getDocCnhPath());
        addDocIfPresent(docs, "B.O.", event.getDocBoPath());
        addDocIfPresent(docs, "Comprovante de Residência", event.getDocComprovanteResidenciaPath());
        addDocIfPresent(docs, "Termo de Abertura", event.getDocTermoAberturaPath());

        // Adiciona documentos de terceiros, se houver
        if (Boolean.TRUE.equals(event.getTerceiroEnvolvido())) {
            addDocIfPresent(docs, "CNH do Terceiro", event.getDocTerceiroCnhPath());
            addDocIfPresent(docs, "CRLV do Terceiro", event.getDocTerceiroCrlvPath());
            addDocIfPresent(docs, "Outros Docs do Terceiro", event.getDocTerceiroOutrosPath());
        }

        return docs;
    }

    private void addDocIfPresent(List<Map<String, String>> docs, String label, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        Map<String, String> doc = new HashMap<>();
        doc.put("label", label);
        doc.put("url", toPublicUrl(path));
        doc.put("filename", Paths.get(path).getFileName().toString());
        docs.add(doc);
    }

    private boolean anexarNovasFotos(Event event, MultipartFile[] novasFotos, Authentication authentication) {
        if (novasFotos == null || novasFotos.length == 0) {
            return false;
        }

        List<VistoriaFoto> fotos = new ArrayList<>();
        for (MultipartFile arquivo : novasFotos) {
            if (arquivo == null || arquivo.isEmpty()) {
                continue;
            }
            String path = fileStorageService.storeFile(arquivo);
            if (path == null) {
                continue;
            }
            VistoriaFoto foto = new VistoriaFoto();
            foto.setFotoPath(path.replace("\\", "/"));
            fotos.add(foto);
        }

        if (fotos.isEmpty()) {
            return false;
        }

        Vistoria vistoria = vistoriaService.findLatestWithPhotosByEventId(event.getId())
                .orElseGet(() -> {
                    Vistoria nova = new Vistoria();
                    nova.setEvent(event);
                    nova.setUsuarioCriacao(authentication != null ? authentication.getName() : "Sistema");
                    return vistoriaService.create(nova);
                });

        int ordem = vistoria.getFotos() != null ? vistoria.getFotos().size() : 0;
        for (VistoriaFoto foto : fotos) {
            foto.setOrdem(++ordem);
            foto.setVistoria(vistoria);
        }

        vistoriaService.updateWithNewPhotos(vistoria.getId(), vistoria.getObservacoes(), fotos);
        return true;
    }

    private String resumir(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > 80 ? texto.substring(0, 80) + "..." : texto;
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
    }

    private String formatHora(Integer hora) {
        if (hora == null) {
            return null;
        }
        int horas = hora / 100;
        int minutos = hora % 100;
        return String.format("%02d:%02d", horas, minutos);
    }

    private String resolveStatusBadgeClass(Status status) {
        if (status == null) {
            return "bg-secondary";
        }
        return switch (status.getPhase()) {
            case 2 -> "bg-warning text-dark";
            case 3 -> "bg-info";
            case 4 -> "bg-primary";
            case 5 -> "bg-success";
            default -> "bg-secondary";
        };
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
