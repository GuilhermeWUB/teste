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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final int MAX_PAGES = 4;
    private static final int DPI = 150;
    private static final int MAX_RETRIES = 3; // Insiste 3x se o Google der erro

    @Value("${gemini.api.key:}")
    private String apiKey;

    // Agora sim: Padrão 2.5 Flash como você pediu
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
        this.objectMapper = new ObjectMapper();
        this.vehicleRepository = vehicleRepository;
        this.partnerRepository = partnerRepository;
        this.eventRepository = eventRepository;
        this.paymentRepository = paymentRepository;
        this.legalProcessRepository = legalProcessRepository;
    }

    // --- EXTRAÇÃO PDF (Mantido igual) ---
    public ExtractedDataDto extractDataFromPdf(MultipartFile pdfFile) throws Exception {
        logger.info("🔥 Iniciando extração PDF...");
        List<String> base64Images = convertPdfPagesToBase64Images(pdfFile);
        if (base64Images.isEmpty()) throw new RuntimeException("PDF vazio.");
        String prompt = buildExtractionPrompt();
        String jsonResponse = callGeminiApiWithRetry(prompt, base64Images, 0.1, true);
        return parseGeminiResponse(jsonResponse);
    }

    /**
     * ANÁLISE RAG - MODO SEM LIMITES + RETRY 🚀
     */
    @Transactional(readOnly = true)
    public String analisarDadosComRAG(String perguntaUsuario) {
        logger.info("🧠 Iniciando análise FULL SCAN para: '{}'", perguntaUsuario);

        try {
            StringBuilder contextoGlobal = new StringBuilder();

            // Carrega tudo. Com 20k registros, o Java tira de letra.
            // A conversão para CSV é rápida.
            contextoGlobal.append(convertListToCsv("VEÍCULOS", safeFindAll(vehicleRepository)));
            contextoGlobal.append("\n");
            contextoGlobal.append(convertListToCsv("CLIENTES", safeFindAll(partnerRepository)));
            contextoGlobal.append("\n");
            contextoGlobal.append(convertListToCsv("EVENTOS", safeFindAll(eventRepository)));
            contextoGlobal.append("\n");
            contextoGlobal.append(convertListToCsv("PAGAMENTOS", safeFindAll(paymentRepository)));

            // Prompt ajustado: "Não limite a lista"
            String promptFinal = buildNoLimitPrompt(perguntaUsuario, contextoGlobal.toString());

            // Chama com Retry para segurar o erro 503
            return callGeminiApiWithRetry(promptFinal, null, 0.3, false);

        } catch (Exception e) {
            logger.error("Erro fatal na análise IA após tentativas", e);
            return "<div class='ai-alert ai-alert-error'><b>Erro de Capacidade:</b> O volume de dados é muito grande e o servidor da IA está instável no momento. Tente ser mais específico na pergunta.</div>";
        }
    }

    // --- LÓGICA DE RETRY (O SEGREDO PRO 503) ---

    private String callGeminiApiWithRetry(String prompt, List<String> base64Images, double temperature, boolean jsonMode) {
        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            try {
                attempts++;
                return callGeminiApi(prompt, base64Images, temperature, jsonMode);

            } catch (HttpServerErrorException.ServiceUnavailable | ResourceAccessException e) {
                // Se der 503 (Overloaded) ou Timeout
                logger.warn("⚠️ Gemini sobrecarregado (Tentativa {}/{}). Esperando...", attempts, MAX_RETRIES);
                if (attempts == MAX_RETRIES) throw e; // Se foi a última, explode o erro

                try {
                    Thread.sleep(2000L * attempts); // Espera 2s, 4s, 6s...
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                // Outros erros (400, 401) não adianta tentar de novo
                logger.error("❌ Erro irrecuperável na API Gemini", e);
                throw new RuntimeException("Erro na comunicação com a IA: " + e.getMessage());
            }
        }
        throw new RuntimeException("Serviço indisponível após várias tentativas.");
    }

    // --- PROMPT SEM LIMITES ---

    private String buildNoLimitPrompt(String pergunta, String dadosCsv) {
        return """
                Você é um Assistente Administrativo da UB.
                
                CONTEXTO (DADOS DO SISTEMA):
                %s
                
                PERGUNTA DO USUÁRIO: "%s"
                
                REGRAS OBRIGATÓRIAS:
                1. **SEM RESUMOS:** Se a resposta tiver 100, 1000 ou 5000 itens, LISTE TODOS eles. Não use frases como "aqui estão os 5 primeiros". O usuário quer a lista completa.
                2. **Formatação:** Use uma tabela HTML (`<table class='table table-striped'>`) para listas longas. É mais organizado.
                3. **Linguagem:** Fale como um humano (sem termos de TI).
                4. **Precisão:** Se pedirem "carros pretos", filtre exatamente pela coluna cor.
                5. **Limitação Técnica:** Se a lista for gigantesca e você não conseguir gerar tudo por limite de tamanho de resposta, liste o máximo possível e avise no final: "A lista continua..."
                """.formatted(dadosCsv, pergunta);
    }

    // --- REFLECTION & CSV (Otimizado para não quebrar) ---

    private <T> String convertListToCsv(String tituloTabela, List<T> lista) {
        if (lista == null || lista.isEmpty()) return "";
        StringBuilder csv = new StringBuilder("\n--- " + tituloTabela + " ---\n");

        try {
            Class<?> clazz = lista.get(0).getClass();
            List<Field> fields = getAllFields(clazz);

            // Cabeçalho
            csv.append(fields.stream().map(Field::getName).collect(Collectors.joining(","))).append("\n");

            for (T item : lista) {
                List<String> valores = new ArrayList<>();
                for (Field field : fields) {
                    try {
                        field.setAccessible(true);
                        Object val = field.get(item);
                        if (val == null) valores.add("");
                        else if (isEntity(val)) valores.add(extrairNome(val));
                        else valores.add(sanitize(val.toString()));
                    } catch (Exception e) { valores.add("-"); }
                }
                csv.append(String.join(",", valores)).append("\n");
            }
        } catch (Exception e) { return ""; }
        return csv.toString();
    }

    // --- MÉTODOS DE APOIO (Igual ao anterior) ---

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

    private boolean isEntity(Object obj) {
        return obj.getClass().getName().startsWith("com.necsus");
    }

    private String extrairNome(Object obj) {
        try {
            Field f = ReflectionUtils.findField(obj.getClass(), "name");
            if (f == null) f = ReflectionUtils.findField(obj.getClass(), "titulo");
            if (f == null) f = ReflectionUtils.findField(obj.getClass(), "model");
            if (f != null) { f.setAccessible(true); return sanitize(f.get(obj).toString()); }
            return "Ref";
        } catch (Exception e) { return "Ref"; }
    }

    private String sanitize(String s) {
        return s.replaceAll("[\\n\\r,;]", " ").trim();
    }

    private <T> List<T> safeFindAll(org.springframework.data.jpa.repository.JpaRepository<T, ?> repo) {
        try { return repo.findAll(); } catch (Exception e) { return Collections.emptyList(); }
    }

    private String callGeminiApi(String prompt, List<String> base64Images, double temperature, boolean jsonMode) {
        String resolvedApiKey = resolveApiKey();
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", modelName, resolvedApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        if (base64Images != null) {
            for (String img : base64Images) {
                parts.add(Map.of("inline_data", Map.of("mime_type", "image/png", "data", img)));
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

    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) return "A IA não retornou dados.";
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText().trim();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private List<String> convertPdfPagesToBase64Images(MultipartFile pdfFile) throws IOException {
        List<String> base64Images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pagesToProcess = Math.min(document.getNumberOfPages(), MAX_PAGES);
            for (int i = 0; i < pagesToProcess; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                base64Images.add(Base64.getEncoder().encodeToString(baos.toByteArray()));
            }
        }
        return base64Images;
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
}