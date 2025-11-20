package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller REST para Relatórios Inteligentes com IA (RAG). 🧠
 * * Agora focado em análise de dados (Data Analysis) e não mais em geração de SQL.
 * O retorno é um HTML formatado pela própria IA.
 */
@RestController
@RequestMapping("/api/relatorios-ia")
public class GenerativeReportController {

    private static final Logger logger = LoggerFactory.getLogger(GenerativeReportController.class);

    private final GeminiService geminiService;

    public GenerativeReportController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Endpoint de Análise de Dados (RAG)
     * * Recebe uma pergunta, o backend carrega os dados, manda pro Gemini
     * e retorna a análise pronta em HTML.
     */
    @PostMapping("/analisar") // Mudei para /analisar para refletir a nova lógica
    public ResponseEntity<Map<String, Object>> analisarDados(@RequestBody Map<String, String> requestBody) {
        Map<String, Object> response = new HashMap<>();

        try {
            String pergunta = requestBody.get("pergunta");

            if (pergunta == null || pergunta.trim().isEmpty()) {
                response.put("sucesso", false);
                response.put("mensagem", "A pergunta é obrigatória.");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("🔍 Iniciando análise IA para: {}", pergunta);

            // Chama o método RAG (que retorna String/HTML)
            String analiseHtml = geminiService.analisarDadosComRAG(pergunta);

            // Resposta de sucesso
            response.put("sucesso", true);
            response.put("html", analiseHtml); // O front vai pegar isso e dar um .innerHTML

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erro na análise IA", e);
            response.put("sucesso", false);
            response.put("html", "<div class='alert alert-danger'>Erro ao analisar dados: " + e.getMessage() + "</div>");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health check
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "online", "mode", "RAG Analysis"));
    }
}