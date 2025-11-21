package com.necsus.necsusspring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.necsus.necsusspring.dto.ExtractedDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String modelName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
    }

    // DTOs internos
    private record GeminiRequest(List<Content> contents) {}
    private record Content(List<Part> parts) {}
    private record Part(String text, InlineData inlineData) {}
    private record InlineData(String mimeType, String data) {}
    private record GeminiResponse(List<Candidate> candidates) {}
    private record Candidate(Content content) {}

    /**
     * 1. Analisa danos veiculares (VISTORIA)
     */
    public String analisarVistoria(String descricaoEvento, List<MultipartFile> fotos) {
        if (isApiConfigMissing()) return "Erro: API Key não configurada.";
        String url = getApiUrl();

        try {
            List<Part> parts = new ArrayList<>();
            parts.add(new Part("Analise as fotos e relato: " + descricaoEvento, null));

            if (fotos != null) {
                for (MultipartFile foto : fotos) {
                    if (foto.getSize() > 0) {
                        String base64 = Base64.getEncoder().encodeToString(foto.getBytes());
                        parts.add(new Part(null, new InlineData("image/jpeg", base64)));
                    }
                }
            }
            return callGeminiSimple(url, parts);
        } catch (Exception e) {
            return "Erro técnico: " + e.getMessage();
        }
    }

    /**
     * 2. Extrai dados de Nota Fiscal/Boleto (FINANCEIRO)
     */
    public ExtractedDataDto extractDataFromPdf(MultipartFile file) {
        if (isApiConfigMissing()) throw new RuntimeException("API Key não configurada.");
        String url = getApiUrl();

        try {
            String prompt = """
                Analise este documento. Retorne JSON PURO com chaves:
                {
                    "numeroNota": "string",
                    "dataEmissao": "YYYY-MM-DD",
                    "valor": "0.00",
                    "placa": "string"
                }
                """;

            List<Part> parts = new ArrayList<>();
            parts.add(new Part(prompt, null));

            String mimeType = file.getContentType() != null ? file.getContentType() : "application/pdf";
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            parts.add(new Part(null, new InlineData(mimeType, base64)));

            String jsonResponse = callGeminiSimple(url, parts);

            if (jsonResponse == null) return null;

            String cleanJson = jsonResponse.replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(cleanJson, ExtractedDataDto.class);

        } catch (Exception e) {
            log.error("Erro ao ler PDF", e);
            return null;
        }
    }

    /**
     * 3. Analisa Dados / Pergunta Livre (O QUE ESTAVA FALTANDO)
     * Retorna String formatada em HTML para exibir no front.
     */
    public String analisarDadosComRAG(String pergunta) {
        if (isApiConfigMissing()) return "<p class='text-danger'>Erro: API Key não configurada.</p>";
        String url = getApiUrl();

        try {
            // Prompt instruindo a IA a responder em HTML limpo
            String prompt = """
                Você é um assistente especialista do sistema NECSUS.
                Responda à pergunta do usuário: "%s"
                
                Diretrizes:
                1. Responda de forma profissional e direta.
                2. Se a pergunta for sobre dados que você não tem acesso, explique que precisa do contexto.
                3. IMPORTANTE: Formate a resposta usando tags HTML simples (<p>, <b>, <ul>, <li>).
                4. NÃO use Markdown (não use ** ou ##). NÃO use tags <html> ou <body>.
                """.formatted(pergunta);

            List<Part> parts = new ArrayList<>();
            parts.add(new Part(prompt, null));

            String respostaHtml = callGeminiSimple(url, parts);

            if (respostaHtml == null) return "<p>A IA não retornou resposta.</p>";

            // Remove blocos de código se a IA teimar em mandar
            return respostaHtml.replace("```html", "").replace("```", "").trim();

        } catch (Exception e) {
            log.error("Erro no RAG", e);
            return "<p class='text-danger'>Erro ao processar sua pergunta: " + e.getMessage() + "</p>";
        }
    }

    // Métodos Auxiliares Privados

    private boolean isApiConfigMissing() {
        return apiKey == null || apiKey.isBlank();
    }

    private String getApiUrl() {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
    }

    private String callGeminiSimple(String url, List<Part> parts) {
        GeminiRequest request = new GeminiRequest(List.of(new Content(parts)));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(url, entity, GeminiResponse.class);

        if (response.getBody() != null && response.getBody().candidates() != null && !response.getBody().candidates().isEmpty()) {
            return response.getBody().candidates().get(0).content().parts().get(0).text();
        }
        return null;
    }
}