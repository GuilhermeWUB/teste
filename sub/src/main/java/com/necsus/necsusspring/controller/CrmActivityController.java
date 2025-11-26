package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.ActivityRequest;
import com.necsus.necsusspring.model.ActivityStatus;
import com.necsus.necsusspring.model.ActivityType;
import com.necsus.necsusspring.model.CrmActivity;
import com.necsus.necsusspring.service.CrmActivityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crm/api/atividades")
@PreAuthorize("hasAnyRole('ADMIN', 'COMERCIAL', 'CLOSERS', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class CrmActivityController {

    private final CrmActivityService activityService;

    public CrmActivityController(CrmActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<List<CrmActivity>> getAllActivities() {
        List<CrmActivity> activities = activityService.findAll();
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrmActivity> getActivityById(@PathVariable Long id) {
        return activityService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CrmActivity>> getActivitiesByStatus(@PathVariable ActivityStatus status) {
        List<CrmActivity> activities = activityService.findByStatus(status);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<CrmActivity>> getActivitiesByTipo(@PathVariable ActivityType tipo) {
        List<CrmActivity> activities = activityService.findByTipo(tipo);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/venda/{saleId}")
    public ResponseEntity<List<CrmActivity>> getActivitiesBySale(@PathVariable Long saleId) {
        List<CrmActivity> activities = activityService.findBySaleId(saleId);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/responsavel/{responsavel}")
    public ResponseEntity<List<CrmActivity>> getActivitiesByResponsavel(@PathVariable String responsavel) {
        List<CrmActivity> activities = activityService.findByResponsavel(responsavel);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/agendadas")
    public ResponseEntity<List<CrmActivity>> getScheduledActivities(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<CrmActivity> activities = activityService.findByDataAgendadaBetween(start, end);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/recentes")
    public ResponseEntity<List<CrmActivity>> getRecentActivities() {
        List<CrmActivity> activities = activityService.findRecent();
        return ResponseEntity.ok(activities);
    }

    @PostMapping
    public ResponseEntity<CrmActivity> createActivity(@Valid @RequestBody ActivityRequest request) {
        CrmActivity created = activityService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CrmActivity> updateActivity(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        try {
            CrmActivity updated = activityService.update(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CrmActivity> updateActivityStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String statusStr = body.get("status");
            if (statusStr == null || statusStr.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            ActivityStatus newStatus = ActivityStatus.valueOf(statusStr);
            CrmActivity updated = activityService.updateStatus(id, newStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
        try {
            activityService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
