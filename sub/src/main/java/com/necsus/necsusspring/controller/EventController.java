package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.EventBoardSnapshot;
import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.EventService;
import com.necsus.necsusspring.service.EventObservationHistoryService;
import com.necsus.necsusspring.service.EventDescriptionHistoryService;
import com.necsus.necsusspring.service.FileStorageService;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.*;

@Controller
@RequestMapping("/events")
public class EventController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;
    private final PartnerService partnerService;
    private final VehicleService vehicleService;
    private final FileStorageService fileStorageService;
    private final EventObservationHistoryService observationHistoryService;
    private final EventDescriptionHistoryService descriptionHistoryService;

    public EventController(EventService eventService,
                           PartnerService partnerService,
                           VehicleService vehicleService,
                           FileStorageService fileStorageService,
                           EventObservationHistoryService observationHistoryService,
                           EventDescriptionHistoryService descriptionHistoryService) {
        this.eventService = eventService;
        this.partnerService = partnerService;
        this.vehicleService = vehicleService;
        this.fileStorageService = fileStorageService;
        this.observationHistoryService = observationHistoryService;
        this.descriptionHistoryService = descriptionHistoryService;
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

    @ModelAttribute("partners")
    public List<Partner> partners() {
        return partnerService.getAllPartners();
    }

    @GetMapping
    public String listEvents() {
        return "redirect:/events/board";
    }

    /**
     * Exibe o board estilo Trello
     */
    @GetMapping("/board")
    public String showBoard(Model model) {
        EventBoardSnapshot snapshot = eventService.getBoardSnapshot();

        model.addAttribute("eventsByStatus", snapshot.eventsByStatus());
        model.addAttribute("boardCounters", snapshot.counters());
        model.addAttribute("boardCards", snapshot.cards());
        model.addAttribute("totalEvents", snapshot.totalEvents());

        List<Map<String, String>> statusMetadata = Arrays.stream(Status.values())
                .map(status -> Map.of(
                        "code", status.name(),
                        "label", status.getDisplayName()
                ))
                .toList();
        model.addAttribute("statusMetadata", statusMetadata);

        // Usando template com HTML inline dentro do layout:fragment
        return "board_eventos";
    }

    /**
     * API REST para buscar eventos por status (usado pelo board)
     * Retorna DTOs ao invés de entidades para evitar problemas de serialização com vehicle null
     */
    @GetMapping("/api/by-status/{status}")
    @ResponseBody
    public ResponseEntity<List<com.necsus.necsusspring.dto.EventBoardCardDto>> getEventsByStatus(@PathVariable Status status) {
        try {
            logger.info("[KANBAN API] Buscando eventos com status: {}", status);
            List<Event> events = eventService.listByStatus(status);
            logger.info("[KANBAN API] Encontrados {} eventos para status {}", events.size(), status);

            List<com.necsus.necsusspring.dto.EventBoardCardDto> dtos = events.stream()
                    .map(com.necsus.necsusspring.dto.EventBoardCardDto::from)
                    .toList();

            logger.info("[KANBAN API] Retornando {} DTOs para o frontend", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("[KANBAN API] Erro ao buscar eventos por status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/board")
    @ResponseBody
    public ResponseEntity<EventBoardSnapshot> getBoardSnapshotApi() {
        return ResponseEntity.ok(eventService.getBoardSnapshot());
    }

    /**
     * API REST para atualizar status do evento (drag & drop no board)
     */
    @PutMapping("/api/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateEventStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String statusStr = payload.get("status");
            Status newStatus = Status.valueOf(statusStr);

            Event updated = eventService.updateStatus(id, newStatus);

            logger.info("Status do evento {} atualizado para {}", id, newStatus);

            // Converte para DTO para evitar problemas de serialização JSON
            com.necsus.necsusspring.dto.EventBoardCardDto eventDto =
                com.necsus.necsusspring.dto.EventBoardCardDto.from(updated);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Status atualizado com sucesso");
            response.put("event", eventDto);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Status inválido: {}", payload.get("status"));
            return ResponseEntity.badRequest().body(Map.of("error", "Status inválido"));
        } catch (Exception e) {
            logger.error("Erro ao atualizar status do evento {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao atualizar status"));
        }
    }

    @GetMapping("/api/vehicles/{partnerId}")
    @ResponseBody
    public ResponseEntity<List<com.necsus.necsusspring.dto.VehicleDTO>> getVehiclesByPartner(@PathVariable Long partnerId) {
        logger.info("[EVENT API] 🚗 Buscando veículos do parceiro ID: {}", partnerId);

        List<Vehicle> vehicles = vehicleService.listByPartnerId(partnerId);
        logger.info("[EVENT API] ✅ Encontrados {} veículos", vehicles.size());

        // Converte as entidades Vehicle para DTOs
        List<com.necsus.necsusspring.dto.VehicleDTO> vehicleDTOs = vehicles.stream()
                .map(com.necsus.necsusspring.dto.VehicleDTO::fromEntity)
                .toList();

        if (!vehicleDTOs.isEmpty()) {
            com.necsus.necsusspring.dto.VehicleDTO firstVehicle = vehicleDTOs.get(0);
            logger.info("[EVENT API] 📋 Primeiro veículo DTO: ID={}, Placa={}, Marca={}, Modelo={}",
                firstVehicle.getId(),
                firstVehicle.getPlaque(),
                firstVehicle.getMaker(),
                firstVehicle.getModel());
            logger.info("[EVENT API] 📦 JSON que será enviado (primeiro veículo): {}", firstVehicle);
        }

        logger.info("[EVENT API] 📤 Retornando {} DTOs para o frontend", vehicleDTOs.size());
        return ResponseEntity.ok(vehicleDTOs);
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Event event = new Event();
        event.setStatus(Status.COMUNICADO); // Status padrão
        // Instanciar partner para permitir binding de partner.id no formulário
        event.setPartner(new Partner());
        // Vehicle agora é opcional, não precisa instanciar
        model.addAttribute("event", event);
        return "cadastro_evento";
    }

    @PostMapping
    public String createEvent(@Valid @ModelAttribute("event") Event event,
                              BindingResult result,
                              @RequestParam(value = "docCrlv", required = false) MultipartFile docCrlv,
                              @RequestParam(value = "docCnh", required = false) MultipartFile docCnh,
                              @RequestParam(value = "docBo", required = false) MultipartFile docBo,
                              @RequestParam(value = "docComprovanteResidencia", required = false) MultipartFile docComprovanteResidencia,
                              @RequestParam(value = "docTermoAbertura", required = false) MultipartFile docTermoAbertura,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        // Validações manuais para garantir que IDs foram enviados
        if (event.getPartner() == null || event.getPartner().getId() == null) {
            result.rejectValue("partner", "NotNull", "Selecione o associado");
        }
        // Vehicle é agora opcional - placa pode ser informada manualmente no campo placaManual
        if (result.hasErrors()) {
            return "cadastro_evento";
        }
        try {
            // Processa upload dos documentos
            if (docCrlv != null && !docCrlv.isEmpty()) {
                String crlvPath = fileStorageService.storeFile(docCrlv);
                event.setDocCrlvPath(crlvPath);
                logger.info("CRLV anexado: {}", crlvPath);
            }

            if (docCnh != null && !docCnh.isEmpty()) {
                String cnhPath = fileStorageService.storeFile(docCnh);
                event.setDocCnhPath(cnhPath);
                logger.info("CNH anexada: {}", cnhPath);
            }

            if (docBo != null && !docBo.isEmpty()) {
                String boPath = fileStorageService.storeFile(docBo);
                event.setDocBoPath(boPath);
                logger.info("B.O. anexado: {}", boPath);
            }

            if (docComprovanteResidencia != null && !docComprovanteResidencia.isEmpty()) {
                String compResPath = fileStorageService.storeFile(docComprovanteResidencia);
                event.setDocComprovanteResidenciaPath(compResPath);
                logger.info("Comprovante de residência anexado: {}", compResPath);
            }

            if (docTermoAbertura != null && !docTermoAbertura.isEmpty()) {
                String termoPath = fileStorageService.storeFile(docTermoAbertura);
                event.setDocTermoAberturaPath(termoPath);
                logger.info("Termo de abertura anexado: {}", termoPath);
            }

            eventService.create(event);
            redirectAttributes.addFlashAttribute("successMessage", "Evento cadastrado com sucesso!");
            return "redirect:/events/board";
        } catch (Exception ex) {
            logger.error("Erro ao criar evento: ", ex);
            model.addAttribute("formError", ex.getMessage() != null ? ex.getMessage() : "Não foi possível salvar o evento. Verifique os campos e tente novamente.");
            return "cadastro_evento";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Event event = eventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        model.addAttribute("event", event);
        return "update_evento";
    }

    @PostMapping("/update/{id}")
    public String updateEvent(@PathVariable Long id,
                              @Valid @ModelAttribute("event") Event event,
                              BindingResult result,
                              RedirectAttributes redirectAttributes,
                              Model model,
                              Authentication authentication) {
        if (result.hasErrors()) {
            return "update_evento";
        }
        event.setId(id);

        // Obtém username do usuário autenticado para rastreamento de histórico
        String username = authentication != null ? authentication.getName() : "Sistema";
        eventService.updateWithHistory(id, event, username);

        redirectAttributes.addFlashAttribute("successMessage", "Evento atualizado com sucesso!");
        return "redirect:/events";
    }

    @PostMapping("/delete/{id}")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Evento removido com sucesso!");
        return "redirect:/events";
    }

    /**
     * API REST para atualização inline de evento
     */
    @PutMapping("/api/{id}/update")
    @ResponseBody
    public ResponseEntity<?> updateEventInline(@PathVariable Long id,
                                                @RequestBody Map<String, Object> updates,
                                                Authentication authentication) {
        try {
            // Obtém username do usuário autenticado para rastreamento de histórico
            String username = authentication != null ? authentication.getName() : "Sistema";
            Event updated = eventService.updatePartialWithHistory(id, updates, username);
            logger.info("Evento {} atualizado inline por {}: {}", id, username, updates.keySet());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Evento atualizado com sucesso",
                "event", updated
            ));
        } catch (Exception e) {
            logger.error("Erro ao atualizar evento {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao atualizar evento"));
        }
    }

    /**
     * API REST para buscar histórico de mudanças
     */
    @GetMapping("/api/{id}/history")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getEventHistory(@PathVariable Long id) {
        try {
            // Retorna lista vazia por enquanto - implementar persistence de histórico
            List<Map<String, Object>> history = new ArrayList<>();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Erro ao buscar histórico do evento {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * API REST para registrar mudança no histórico
     */
    @PostMapping("/api/{id}/history")
    @ResponseBody
    public ResponseEntity<?> logEventChange(@PathVariable Long id, @RequestBody Map<String, Object> change) {
        try {
            logger.info("Registrando mudança no evento {}: {}", id, change);
            // Implementar persistence de histórico aqui se necessário
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Erro ao registrar mudança no evento {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao registrar mudança"));
        }
    }

    /**
     * API REST para exportação PDF
     */
    @GetMapping("/api/export/pdf")
    public ResponseEntity<byte[]> exportToPDF(@RequestParam String ids) {
        try {
            logger.info("Exportando eventos para PDF: {}", ids);

            // Implementação simplificada - retorna placeholder
            String content = "PDF Export - Feature em desenvolvimento\n\nEventos: " + ids;
            byte[] pdfBytes = content.getBytes();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "eventos.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            logger.error("Erro ao exportar PDF: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Download de documento anexado ao evento
     * @param id ID do evento
     * @param docType Tipo do documento (crlv, cnh, bo, comprovante_residencia, termo_abertura)
     * @return Arquivo para download
     */
    @GetMapping("/{id}/download/{docType}")
    @ResponseBody
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @PathVariable String docType) {
        try {
            Event event = eventService.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));

            // Determina qual campo de documento usar baseado no tipo
            String filePath = switch (docType.toLowerCase()) {
                case "crlv" -> event.getDocCrlvPath();
                case "cnh" -> event.getDocCnhPath();
                case "bo" -> event.getDocBoPath();
                case "comprovante_residencia" -> event.getDocComprovanteResidenciaPath();
                case "termo_abertura" -> event.getDocTermoAberturaPath();
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de documento inválido");
            };

            if (filePath == null || filePath.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado");
            }

            // Carregar o arquivo como Resource
            Path file = Paths.get(filePath);
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado ou não pode ser lido");
            }

            // Determinar o tipo de conteúdo
            String contentType = "application/octet-stream";
            String filename = file.getFileName().toString();
            if (filename.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            }

            // Criar nome amigável para o download
            String downloadFilename = switch (docType.toLowerCase()) {
                case "crlv" -> "CRLV_Evento_" + id + getExtension(filename);
                case "cnh" -> "CNH_Evento_" + id + getExtension(filename);
                case "bo" -> "BO_Evento_" + id + getExtension(filename);
                case "comprovante_residencia" -> "Comprovante_Residencia_Evento_" + id + getExtension(filename);
                case "termo_abertura" -> "Termo_Abertura_Evento_" + id + getExtension(filename);
                default -> filename;
            };

            logger.info("Download de documento solicitado - Evento: {}, Tipo: {}, Arquivo: {}", id, docType, filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadFilename + "\"")
                    .body(resource);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (MalformedURLException e) {
            logger.error("Erro ao construir URL do arquivo - Evento: {}, Tipo: {}", id, docType, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao acessar o arquivo");
        } catch (Exception e) {
            logger.error("Erro ao baixar documento - Evento: {}, Tipo: {}", id, docType, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao baixar documento");
        }
    }

    /**
     * Busca o histórico de alterações das observações de um evento
     * @param id ID do evento
     * @return Lista de históricos em formato JSON
     */
    @GetMapping("/{id}/observation-history")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getObservationHistory(@PathVariable Long id) {
        try {
            List<EventObservationHistory> history = observationHistoryService.getHistoryByEventId(id);

            // Converte para formato JSON amigável
            List<Map<String, Object>> historyDtos = new ArrayList<>();
            for (EventObservationHistory entry : history) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", entry.getId());
                dto.put("previousObservation", entry.getPreviousObservation());
                dto.put("newObservation", entry.getNewObservation());
                dto.put("modifiedBy", entry.getModifiedBy());
                dto.put("modifiedAt", entry.getModifiedAt().toString());
                historyDtos.add(dto);
            }

            return ResponseEntity.ok(historyDtos);

        } catch (Exception e) {
            logger.error("Erro ao buscar histórico de observações do evento {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * Busca o histórico de alterações da descrição de um evento
     * @param id ID do evento
     * @return Lista de históricos em formato JSON
     */
    @GetMapping("/{id}/description-history")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDescriptionHistory(@PathVariable Long id) {
        try {
            List<EventDescriptionHistory> history = descriptionHistoryService.getHistoryByEventId(id);

            // Converte para formato JSON amigável
            List<Map<String, Object>> historyDtos = new ArrayList<>();
            for (EventDescriptionHistory entry : history) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", entry.getId());
                dto.put("previousDescription", entry.getPreviousDescription());
                dto.put("newDescription", entry.getNewDescription());
                dto.put("modifiedBy", entry.getModifiedBy());
                dto.put("modifiedAt", entry.getModifiedAt().toString());
                historyDtos.add(dto);
            }

            return ResponseEntity.ok(historyDtos);

        } catch (Exception e) {
            logger.error("Erro ao buscar histórico de descrição do evento {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * Método auxiliar para extrair a extensão do arquivo
     */
    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot);
    }

}