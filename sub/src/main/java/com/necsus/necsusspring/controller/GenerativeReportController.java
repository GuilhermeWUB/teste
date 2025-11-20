package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para geração de relatórios dinâmicos usando IA (Gemini).
 *
 * Este controller permite que usuários façam perguntas em linguagem natural
 * e recebam dados do banco de dados como resposta.
 *
 * IMPORTANTE: Para produção, configure o banco de dados com um usuário
 * que tenha APENAS permissões de leitura (SELECT).
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
     * Endpoint para gerar relatórios baseados em perguntas em linguagem natural.
     *
     * Exemplo de requisição:
     * POST /api/relatorios-ia/gerar
     * {
     *   "pergunta": "Quantos veículos ativos temos?"
     * }
     *
     * Exemplo de resposta:
     * {
     *   "sucesso": true,
     *   "dados": [
     *     {"count": 150}
     *   ],
     *   "mensagem": "Relatório gerado com sucesso",
     *   "sql": "SELECT COUNT(*) as count FROM vehicle WHERE vehicle_status = 'ACTIVE'"
     * }
     *
     * @param requestBody Mapa contendo a chave "pergunta" com a pergunta do usuário
     * @return ResponseEntity com os dados ou mensagem de erro
     */
    @PostMapping("/gerar")
    public ResponseEntity<Map<String, Object>> gerarRelatorio(@RequestBody Map<String, String> requestBody) {
        Map<String, Object> response = new HashMap<>();

        try {
            String pergunta = requestBody.get("pergunta");

            // Validar entrada
            if (pergunta == null || pergunta.trim().isEmpty()) {
                response.put("sucesso", false);
                response.put("mensagem", "A pergunta não pode estar vazia");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Recebida pergunta para relatório: {}", pergunta);

            // Gerar e executar SQL
            List<Map<String, Object>> dados = geminiService.gerarRelatorioPorTexto(pergunta);

            // Preparar resposta de sucesso
            response.put("sucesso", true);
            response.put("dados", dados);
            response.put("mensagem", "Relatório gerado com sucesso");
            response.put("totalLinhas", dados.size());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Configuração ausente (ex: chave da API não configurada)
            logger.error("Configuração incompleta para relatórios IA: {}", e.getMessage());
            response.put("sucesso", false);
            response.put("mensagem", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);

        } catch (SecurityException e) {
            // Erro de segurança (SQL não permitido)
            logger.error("Erro de segurança ao gerar relatório: {}", e.getMessage());
            response.put("sucesso", false);
            response.put("mensagem", "Erro de segurança: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception e) {
            // Erro geral
            logger.error("Erro ao gerar relatório", e);
            response.put("sucesso", false);
            response.put("mensagem", "Erro ao gerar relatório: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint de health check
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "online");
        response.put("servico", "Relatórios com IA");
        return ResponseEntity.ok(response);
    }
}
