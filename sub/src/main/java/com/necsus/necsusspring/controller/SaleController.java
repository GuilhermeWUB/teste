package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.SaleRequest;
import com.necsus.necsusspring.model.Sale;
import com.necsus.necsusspring.model.SaleStatus;
import com.necsus.necsusspring.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crm/api/vendas")
@PreAuthorize("hasAnyRole('ADMIN', 'COMERCIAL', 'CLOSERS', 'DIRETORIA', 'GERENTE', 'GESTOR')")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public ResponseEntity<List<Sale>> getAllSales() {
        List<Sale> sales = saleService.findAll();
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSaleById(@PathVariable Long id) {
        return saleService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Sale> createSale(@Valid @RequestBody SaleRequest request) {
        Sale created = saleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sale> updateSale(@PathVariable Long id, @Valid @RequestBody SaleRequest request) {
        try {
            Sale updated = saleService.update(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Sale> updateSaleStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String statusStr = body.get("status");
            if (statusStr == null || statusStr.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            SaleStatus newStatus = SaleStatus.valueOf(statusStr);
            Sale updated = saleService.updateStatus(id, newStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        try {
            saleService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/concluir")
    public ResponseEntity<Sale> completeSale(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        try {
            Double valorVenda = body.get("valorVenda");
            if (valorVenda == null) {
                return ResponseEntity.badRequest().build();
            }

            Sale completed = saleService.completeSale(id, valorVenda);
            return ResponseEntity.ok(completed);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/concluidas")
    public ResponseEntity<List<Sale>> getConcluidedSales() {
        List<Sale> sales = saleService.findConcluidas();
        return ResponseEntity.ok(sales);
    }
}
