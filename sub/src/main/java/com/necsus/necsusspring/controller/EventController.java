package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Envolvimento;
import com.necsus.necsusspring.model.Event;
import com.necsus.necsusspring.model.Motivo;
import com.necsus.necsusspring.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @ModelAttribute("motivoOptions")
    public Motivo[] motivoOptions() {
        return Motivo.values();
    }

    @ModelAttribute("envolvimentoOptions")
    public Envolvimento[] envolvimentoOptions() {
        return Envolvimento.values();
    }

    @GetMapping
    public String listEvents(Model model) {
        List<Event> events = eventService.listAll();
        model.addAttribute("events", events);
        return "lista_eventos";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Event event = new Event();
        model.addAttribute("event", event);
        return "cadastro_evento";
    }

    @PostMapping
    public String createEvent(@Valid @ModelAttribute("event") Event event,
                              BindingResult result,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (result.hasErrors()) {
            return "cadastro_evento";
        }
        eventService.create(event);
        redirectAttributes.addFlashAttribute("successMessage", "Comunicação cadastrada com sucesso!");
        return "redirect:/events";
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
        redirectAttributes.addFlashAttribute("successMessage", "Comunicação atualizada com sucesso!");
        return "redirect:/events";
    }

    @PostMapping("/delete/{id}")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Comunicação removida com sucesso!");
        return "redirect:/events";
    }
}
