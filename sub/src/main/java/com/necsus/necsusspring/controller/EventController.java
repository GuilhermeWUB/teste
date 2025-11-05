package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.EventBoardSnapshot;
import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.EventService;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/events")
public class EventController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;
    private final PartnerService partnerService;
    private final VehicleService vehicleService;

    public EventController(EventService eventService,
                           PartnerService partnerService,
                           VehicleService vehicleService) {
        this.eventService = eventService;
        this.partnerService = partnerService;
        this.vehicleService = vehicleService;
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

        // Usando template direto sem layout decorator para evitar problemas de renderização
        return "board_eventos_direct";
    }

    /**
     * API REST para buscar eventos por status (usado pelo board)
     * Retorna DTOs ao invés de entidades para evitar problemas de serialização com vehicle null
     */
    @GetMapping("/api/by-status/{status}")
    @ResponseBody
    public ResponseEntity<List<com.necsus.necsusspring.dto.EventBoardCardDto>> getEventsByStatus(@PathVariable Status status) {
        List<Event> events = eventService.listByStatus(status);
        List<com.necsus.necsusspring.dto.EventBoardCardDto> dtos = events.stream()
                .map(com.necsus.necsusspring.dto.EventBoardCardDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Status atualizado com sucesso");
            response.put("event", updated);

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
        event.setStatus(Status.A_FAZER); // Status padrão
        // Instanciar partner para permitir binding de partner.id no formulário
        event.setPartner(new Partner());
        // Vehicle agora é opcional, não precisa instanciar
        model.addAttribute("event", event);
        return "cadastro_evento";
    }

    @PostMapping
    public String createEvent(@Valid @ModelAttribute("event") Event event,
                              BindingResult result,
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
                              Model model) {
        if (result.hasErrors()) {
            return "update_evento";
        }
        event.setId(id);
        eventService.update(id, event);
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
    public ResponseEntity<?> updateEventInline(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            Event updated = eventService.updatePartial(id, updates);
            logger.info("Evento {} atualizado inline: {}", id, updates.keySet());

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

}