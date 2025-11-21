package com.necsus.necsusspring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.necsus.necsusspring.dto.ExtractedDataDto;
import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final int MAX_PAGES = 4;
    private static final int DPI = 150;
    private static final int MAX_RETRIES = 3;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String modelName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Repositórios
    private final VehicleRepository vehicleRepository;
    private final PartnerRepository partnerRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final LegalProcessRepository legalProcessRepository;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper,
                         VehicleRepository vehicleRepository, PartnerRepository partnerRepository,
                         EventRepository eventRepository, PaymentRepository paymentRepository,
                         LegalProcessRepository legalProcessRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.vehicleRepository = vehicleRepository;
        this.partnerRepository = partnerRepository;
        this.eventRepository = eventRepository;
        this.paymentRepository = paymentRepository;
        this.legalProcessRepository = legalProcessRepository;
    }

    // --- DTO Interno para Suportar Múltiplos Formatos (JPG/PNG) ---
    private record GeminiImage(String base64, String mimeType) {}

    // ==================================================================================
    // 1. EXTRAÇÃO DE DADOS DE PDF (Mantido)
    // ==================================================================================
    public ExtractedDataDto extractDataFromPdf(MultipartFile pdfFile) throws Exception {
        logger.info("🔥 Iniciando extração PDF...");

        // Converte PDF para lista de imagens PNG (adaptado para GeminiImage)
        List<GeminiImage> images = convertPdfPagesToImages(pdfFile);

        if (images.isEmpty()) throw new RuntimeException("PDF vazio.");

        String prompt = buildExtractionPrompt();
        String jsonResponse = callGeminiApiWithRetry(prompt, images, 0.1, true);

        return parseGeminiResponse(jsonResponse);
    }

    // ==================================================================================
    // 2. ANÁLISE DE VISTORIA (NOVO - Adicionado conforme pedido)
    // ==================================================================================
    public String analisarVistoria(String descricaoEvento, List<MultipartFile> fotos) {
        logger.info("🚗 Iniciando análise de vistoria...");

        if (fotos == null || fotos.isEmpty()) return "Nenhuma foto fornecida.";

        try {
            List<GeminiImage> images = new ArrayList<>();
            for (MultipartFile foto : fotos) {
                if (foto.getSize() > 0) {
                    String base64 = Base64.getEncoder().encodeToString(foto.getBytes());
                    // Detecta o tipo real (JPEG, PNG, etc)
                    String mime = foto.getContentType() != null ? foto.getContentType() : "image/jpeg";
                    images.add(new GeminiImage(base64, mime));
                }
            }

            String prompt = """
                Atue como um Perito Técnico de Seguros Automotivos e Orçamentista Sênior.
                
                Analise as imagens anexadas deste veículo acidentado em conjunto com o relato do evento.
                Relato do Condutor: "%s"
                
                Gere um RELATÓRIO TÉCNICO PRELIMINAR (em Markdown) contendo:
                1. **Análise de Coerência**: O dano visível nas fotos condiz com o relato? (Sim/Não/Parcialmente). Explique brevemente.
                2. **Lista de Avarias Visíveis**: Liste as peças que aparentam estar danificadas.
                3. **Gravidade Estimada**: (Leve / Média / Alta / Possível Perda Total).
                4. **Sugestão de Reparo**: Para cada peça principal, sugira: Recuperação, Troca ou Pintura.
                
                Seja direto e técnico.
                """.formatted(descricaoEvento != null ? descricaoEvento : "Sem descrição.");

            // Chama API com temperatura 0.4 (ideal para análise criativa mas técnica)
            return callGeminiApiWithRetry(prompt, images, 0.4, false);

        } catch (Exception e) {
            logger.error("Erro na análise de vistoria", e);
            return "Erro técnico ao analisar vistoria: " + e.getMessage();
        }
    }

    // ==================================================================================
    // 3. ANÁLISE RAG (Mantido)
    // ==================================================================================
    @Transactional(readOnly = true)
    public String analisarDadosComRAG(String perguntaUsuario) {
        logger.info("🧠 Iniciando análise FULL SCAN para: '{}'", perguntaUsuario);

        try {
            StringBuilder contextoGlobal = new StringBuilder();
            contextoGlobal.append(convertListToCsv("VEÍCULOS", safeFindAll(vehicleRepository)));
            contextoGlobal.append("\n");
            contextoGlobal.append(convertListToCsv("CLIENTES", safeFindAll(partnerRepository)));
            contextoGlobal.append("\n");
            contextoGlobal.append(convertListToCsv("EVENTOS", safeFindAll(eventRepository)));
            contextoGlobal.append("\n");
            contextoGlobal.append(convertListToCsv("PAGAMENTOS", safeFindAll(paymentRepository)));

            String promptFinal = buildNoLimitPrompt(perguntaUsuario, contextoGlobal.toString());

            // RAG não tem imagens, passa null
            return callGeminiApiWithRetry(promptFinal, null, 0.3, false);

        } catch (Exception e) {
            logger.error("Erro fatal na análise IA", e);
            return "<div class='ai-alert ai-alert-error'><b>Erro de Capacidade:</b> O volume de dados é muito grande.</div>";
        }
    }

    // ==================================================================================
    // MÉTODOS DE API (Atualizados para aceitar GeminiImage)
    // ==================================================================================

    private String callGeminiApiWithRetry(String prompt, List<GeminiImage> images, double temperature, boolean jsonMode) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                attempts++;
                return callGeminiApi(prompt, images, temperature, jsonMode);
            } catch (HttpServerErrorException.ServiceUnavailable | ResourceAccessException e) {
                logger.warn("⚠️ Gemini sobrecarregado (Tentativa {}/{}).", attempts, MAX_RETRIES);
                if (attempts == MAX_RETRIES) throw e;
                try { Thread.sleep(2000L * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                logger.error("❌ Erro irrecuperável na API Gemini", e);
                throw new RuntimeException("Erro na comunicação com a IA: " + e.getMessage());
            }
        }
        throw new RuntimeException("Serviço indisponível.");
    }

    private String callGeminiApi(String prompt, List<GeminiImage> images, double temperature, boolean jsonMode) {
        String resolvedApiKey = resolveApiKey();
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", modelName, resolvedApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        if (images != null) {
            for (GeminiImage img : images) {
                // Aqui está o segredo: usa o mimeType correto de cada imagem
                parts.add(Map.of("inline_data", Map.of(
                        "mime_type", img.mimeType(),
                        "data", img.base64()
                )));
            }
        }
        requestBody.put("contents", List.of(Map.of("parts", parts)));

        Map<String, Object> config = new HashMap<>();
        config.put("temperature", temperature);
        if (jsonMode) config.put("response_mime_type", "application/json");
        requestBody.put("generationConfig", config);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), String.class);
        return extractTextFromResponse(response.getBody());
    }

    // ==================================================================================
    // MÉTODOS AUXILIARES (TODOS ELES ESTÃO AQUI)
    // ==================================================================================

    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) return "A IA não retornou dados.";
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText().trim();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private List<GeminiImage> convertPdfPagesToImages(MultipartFile pdfFile) throws IOException {
        List<GeminiImage> images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pagesToProcess = Math.min(document.getNumberOfPages(), MAX_PAGES);
            for (int i = 0; i < pagesToProcess; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                // PDF convertido vira PNG
                images.add(new GeminiImage(base64, "image/png"));
            }
        }
        return images;
    }

    private ExtractedDataDto parseGeminiResponse(String jsonText) throws Exception {
        String cleanJson = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();
        return objectMapper.readValue(cleanJson, ExtractedDataDto.class);
    }

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) return apiKey;
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null) return envKey;
        throw new IllegalStateException("API Key não encontrada.");
    }

    private String buildExtractionPrompt() { return "Extraia JSON."; }

    private String buildNoLimitPrompt(String pergunta, String dadosCsv) {
        return """
                Você é um Assistente Administrativo da UB.
                CONTEXTO: %s
                PERGUNTA: "%s"
                REGRAS: Use HTML. Liste tudo.
                """.formatted(dadosCsv, pergunta);
    }

    private <T> String convertListToCsv(String tituloTabela, List<T> lista) {
        if (lista == null || lista.isEmpty()) return "";
        StringBuilder csv = new StringBuilder("\n--- " + tituloTabela + " ---\n");
        try {
            Class<?> clazz = lista.get(0).getClass();
            List<Field> fields = getAllFields(clazz);
            csv.append(fields.stream().map(Field::getName).collect(Collectors.joining(","))).append("\n");
            for (T item : lista) {
                List<String> vals = new ArrayList<>();
                for (Field f : fields) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(item);
                        if (v == null) vals.add("");
                        else if (isEntity(v)) vals.add(extrairNome(v));
                        else vals.add(sanitize(v.toString()));
                    } catch (Exception e) { vals.add("-"); }
                }
                csv.append(String.join(",", vals)).append("\n");
            }
        } catch (Exception e) { return ""; }
        return csv.toString();
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredFields())
                    .filter(f -> !Collection.class.isAssignableFrom(f.getType()))
                    .filter(f -> !Map.class.isAssignableFrom(f.getType()))
                    .filter(f -> !f.getName().toLowerCase().contains("photo"))
                    .filter(f -> !f.getName().toLowerCase().contains("image"))
                    .forEach(fields::add);
        }
        return fields;
    }

    private boolean isEntity(Object obj) { return obj.getClass().getName().startsWith("com.necsus"); }

    private String extrairNome(Object obj) {
        try {
            Field f = ReflectionUtils.findField(obj.getClass(), "name");
            if (f == null) f = ReflectionUtils.findField(obj.getClass(), "titulo");
            if (f == null) f = ReflectionUtils.findField(obj.getClass(), "model");
            if (f != null) { f.setAccessible(true); return sanitize(f.get(obj).toString()); }
            return "Ref";
        } catch (Exception e) { return "Ref"; }
    }

    private String sanitize(String s) { return s.replaceAll("[\\n\\r,;]", " ").trim(); }

    private <T> List<T> safeFindAll(org.springframework.data.jpa.repository.JpaRepository<T, ?> repo) {
        try { return repo.findAll(); } catch (Exception e) { return Collections.emptyList(); }
    }
}