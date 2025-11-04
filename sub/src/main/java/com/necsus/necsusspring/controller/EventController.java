package com.necsus.necsusspring.controller;

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

    public EventController(EventService eventService, PartnerService partnerService, VehicleService vehicleService) {
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
        List<Event> events = eventService.listAll();
        model.addAttribute("events", events);
        return "board_eventos";
    }

    /**
     * API REST para buscar eventos por status (usado pelo board)
     */
    @GetMapping("/api/by-status/{status}")
    @ResponseBody
    public ResponseEntity<List<Event>> getEventsByStatus(@PathVariable Status status) {
        List<Event> events = eventService.listByStatus(status);
        return ResponseEntity.ok(events);
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
    public ResponseEntity<List<Vehicle>> getVehiclesByPartner(@PathVariable Long partnerId) {
        List<Vehicle> vehicles = vehicleService.listByPartnerId(partnerId);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Event event = new Event();
        event.setStatus(Status.A_FAZER); // Status padrão
        // Instanciar objetos aninhados para permitir binding de partner.id e vehicle.id no formulário
        event.setPartner(new Partner());
        event.setVehicle(new Vehicle());
        model.addAttribute("event", event);
        model.addAttribute("vehicles", List.of()); // Inicia com lista vazia
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
        if (event.getVehicle() == null || event.getVehicle().getId() == null) {
            result.rejectValue("vehicle", "NotNull", "Selecione uma placa válida da lista");
        }
        if (result.hasErrors()) {
            // Recarrega opções de veículos quando o formulário volta com erro
            if (event.getPartner() != null && event.getPartner().getId() != null) {
                model.addAttribute("vehicles", vehicleService.listByPartnerId(event.getPartner().getId()));
            } else {
                model.addAttribute("vehicles", List.of());
            }
            return "cadastro_evento";
        }
        try {
            eventService.create(event);
            redirectAttributes.addFlashAttribute("successMessage", "Evento cadastrado com sucesso!");
            return "redirect:/events/board";
        } catch (Exception ex) {
            model.addAttribute("formError", ex.getMessage() != null ? ex.getMessage() : "Não foi possível salvar o evento. Verifique os campos e tente novamente.");
            if (event.getPartner() != null && event.getPartner().getId() != null) {
                model.addAttribute("vehicles", vehicleService.listByPartnerId(event.getPartner().getId()));
            } else {
                model.addAttribute("vehicles", List.of());
            }
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
}