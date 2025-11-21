package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.AmbienteNfe;
import com.necsus.necsusspring.model.BillToPay;
import com.necsus.necsusspring.model.IncomingInvoice;
import com.necsus.necsusspring.model.IncomingInvoiceStatus;
import com.necsus.necsusspring.model.NfeConfig;
import com.necsus.necsusspring.repository.IncomingInvoiceRepository;
import com.necsus.necsusspring.service.InvoiceProcessorService;
import com.necsus.necsusspring.service.NfeConfigService;
import com.necsus.necsusspring.service.NfeIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para gerenciar o módulo de NFe
 */
@RestController
@RequestMapping("/api/nfe")
@RequiredArgsConstructor
@Slf4j
public class NfeController {

    private final NfeConfigService nfeConfigService;
    private final NfeIntegrationService nfeIntegrationService;
    private final InvoiceProcessorService invoiceProcessorService;
    private final IncomingInvoiceRepository incomingInvoiceRepository;

    /**
     * POST /api/nfe/config - Salva ou atualiza a configuração com upload de certificado
     */
    @PostMapping("/config")
    public ResponseEntity<?> saveConfig(
            @RequestParam("cnpj") String cnpj,
            @RequestParam("senha") String senha,
            @RequestParam(value = "uf", defaultValue = "SP") String uf,
            @RequestParam(value = "ambiente", defaultValue = "HOMOLOGACAO") String ambiente,
            @RequestParam("certificado") MultipartFile certificadoFile
    ) {
        try {
            log.info("Recebendo configuração NFe para CNPJ: {}", cnpj);

            if (certificadoFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Certificado é obrigatório");
            }

            AmbienteNfe ambienteNfe = AmbienteNfe.valueOf(ambiente.toUpperCase());

            NfeConfig config = nfeConfigService.saveConfigWithCertificate(
                    cnpj, senha, uf, ambienteNfe, certificadoFile);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Configuração salva com sucesso!");
            response.put("config", config);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao salvar configuração: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erro ao salvar configuração: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/nfe/config - Retorna a configuração ativa
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        return nfeConfigService.getConfig()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/nfe/sync - Força uma sincronização manual com a SEFAZ
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncManual() {
        try {
            log.info("Sincronização manual solicitada");

            int notasImportadas = nfeIntegrationService.sincronizarManual();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sincronização concluída");
            response.put("notasImportadas", notasImportadas);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro na sincronização: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erro na sincronização: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/nfe/inbox - Lista as notas na caixa de entrada (paginado)
     */
    @GetMapping("/inbox")
    public ResponseEntity<Page<IncomingInvoice>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<IncomingInvoice> invoices;

        if (status != null && !status.isEmpty()) {
            IncomingInvoiceStatus statusEnum = IncomingInvoiceStatus.valueOf(status.toUpperCase());
            invoices = incomingInvoiceRepository.findByStatusOrderByImportedAtDesc(statusEnum, pageable);
        } else {
            invoices = incomingInvoiceRepository.findAll(pageable);
        }

        return ResponseEntity.ok(invoices);
    }

    /**
     * GET /api/nfe/inbox/pendentes - Lista apenas notas pendentes (sem paginação)
     */
    @GetMapping("/inbox/pendentes")
    public ResponseEntity<List<IncomingInvoice>> getPendentes() {
        List<IncomingInvoice> pendentes = incomingInvoiceRepository.findAllPendentes();
        return ResponseEntity.ok(pendentes);
    }

    /**
     * GET /api/nfe/inbox/{id} - Busca uma nota específica
     */
    @GetMapping("/inbox/{id}")
    public ResponseEntity<IncomingInvoice> getInvoiceById(@PathVariable Long id) {
        return incomingInvoiceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/nfe/{id}/process - Transforma uma nota em conta a pagar
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<?> processInvoice(@PathVariable Long id) {
        try {
            log.info("Processando nota ID: {}", id);

            BillToPay conta = invoiceProcessorService.processarNota(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Nota processada com sucesso!");
            response.put("billToPay", conta);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao processar nota: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * POST /api/nfe/{id}/ignore - Marca uma nota como ignorada
     */
    @PostMapping("/{id}/ignore")
    public ResponseEntity<?> ignoreInvoice(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Usuário optou por ignorar") String motivo
    ) {
        try {
            log.info("Ignorando nota ID: {}", id);

            invoiceProcessorService.ignorarNota(id, motivo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Nota marcada como ignorada");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao ignorar nota: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * POST /api/nfe/{id}/reprocess - Marca uma nota para reprocessamento
     */
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<?> reprocessInvoice(@PathVariable Long id) {
        try {
            log.info("Reprocessando nota ID: {}", id);

            invoiceProcessorService.reprocessarNota(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Nota marcada para reprocessamento");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao reprocessar nota: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * POST /api/nfe/process-all - Processa todas as notas pendentes em lote
     */
    @PostMapping("/process-all")
    public ResponseEntity<?> processAllPendentes() {
        try {
            log.info("Processando todas as notas pendentes");

            int processadas = invoiceProcessorService.processarNotasPendentes();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Processamento em lote concluído");
            response.put("notasProcessadas", processadas);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro no processamento em lote: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/nfe/stats - Retorna estatísticas da caixa de entrada
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long pendentes = incomingInvoiceRepository.countByStatus(IncomingInvoiceStatus.PENDENTE);
        long processadas = incomingInvoiceRepository.countByStatus(IncomingInvoiceStatus.PROCESSADA);
        long ignoradas = incomingInvoiceRepository.countByStatus(IncomingInvoiceStatus.IGNORADA);
        long total = incomingInvoiceRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("pendentes", pendentes);
        stats.put("processadas", processadas);
        stats.put("ignoradas", ignoradas);

        return ResponseEntity.ok(stats);
    }

    /**
     * DELETE /api/nfe/config/{id} - Deleta a configuração
     */
    @DeleteMapping("/config/{id}")
    public ResponseEntity<?> deleteConfig(@PathVariable Long id) {
        try {
            nfeConfigService.deleteConfig(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Configuração deletada com sucesso");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao deletar configuração: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
