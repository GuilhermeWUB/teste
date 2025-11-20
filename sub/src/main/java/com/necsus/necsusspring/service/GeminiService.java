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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço Gemini 2.5 Flash - Versão RAG (Análise de Dados Reais) 🧠📊
 */
@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final int MAX_PAGES = 4;
    private static final int DPI = 150;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String modelName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Injeção dos Repositórios para buscar os dados REAIS
    private final VehicleRepository vehicleRepository;
    private final PartnerRepository partnerRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final LegalProcessRepository legalProcessRepository;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper,
                         VehicleRepository vehicleRepository, PartnerRepository partnerRepository,
                         EventRepository eventRepository, PaymentRepository paymentRepository,
                         LegalProcessRepository legalProcessRepository) {
        this.restTemplate = new RestTemplate(); // Ou use o injetado se preferir
        this.objectMapper = new ObjectMapper();
        this.vehicleRepository = vehicleRepository;
        this.partnerRepository = partnerRepository;
        this.eventRepository = eventRepository;
        this.paymentRepository = paymentRepository;
        this.legalProcessRepository = legalProcessRepository;
    }

    /**
     * Extração de PDF (Mantido igual, pois funciona)
     */
    public ExtractedDataDto extractDataFromPdf(MultipartFile pdfFile) throws Exception {
        logger.info("🔥 Iniciando extração com Gemini: {}", pdfFile.getOriginalFilename());
        List<String> base64Images = convertPdfToBase64Images(pdfFile);

        if (base64Images.isEmpty()) throw new RuntimeException("PDF vazio.");

        String prompt = buildExtractionPrompt();
        String jsonResponse = callGeminiApi(prompt, base64Images, 0.1, true); // Temp baixa para precisão
        return parseGeminiResponse(jsonResponse);
    }

    /**
     * NOVA ABORDAGEM: RAG (Contexto Completo)
     * Em vez de gerar SQL, nós pegamos os dados, formatamos e pedimos a análise.
     */
    public String analisarDadosComRAG(String perguntaUsuario) {
        logger.info("🧠 Iniciando análise RAG para: '{}'", perguntaUsuario);

        // 1. Buscar dados do banco (Carrega tudo ou filtra os recentes)
        // Dica: Se tiver MUITOS dados, use Pageable ou filtre os últimos 1000 registros
        List<Vehicle> veiculos = vehicleRepository.findAll();
        List<Partner> parceiros = partnerRepository.findAll();
        List<Event> eventos = eventRepository.findAll();
        List<Payment> pagamentos = paymentRepository.findAll();

        // 2. Transformar dados em Texto (CSV Otimizado para economizar tokens)
        String dadosContexto = montarContextoDeDados(veiculos, parceiros, eventos, pagamentos);

        // 3. Montar o Prompt Analítico
        String promptFinal = buildDataAnalysisPrompt(perguntaUsuario, dadosContexto);

        // 4. Chamar o Gemini (Como é análise criativa, temperatura pode ser 0.3 ou 0.4)
        try {
            // Chama a API sem imagens, apenas texto
            String resposta = callGeminiApi(promptFinal, null, 0.4, false);

            // Se a resposta vier com markdown de HTML, limpamos ou deixamos para o front renderizar
            return resposta;
        } catch (Exception e) {
            logger.error("Erro na análise RAG", e);
            return "Desculpe, não consegui analisar os dados no momento. Erro: " + e.getMessage();
        }
    }

    // --- MONTAGEM DE CONTEXTO (CSV) ---

    private String montarContextoDeDados(List<Vehicle> veiculos, List<Partner> parceiros, List<Event> eventos, List<Payment> pagamentos) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        sb.append("--- TABELA: VEÍCULOS ---\n");
        sb.append("ID,MODELO,PLACA,STATUS,ID_PARCEIRO\n");
        for (Vehicle v : veiculos) {
            sb.append(String.format("%d,%s,%s,%s,%s\n",
                    v.getId(),
                    sanitize(v.getModel()),
                    v.getPlaque(),
                    v.getVehicleStatus(),
                    (v.getPartner() != null ? v.getPartner().getId() : "N/A")
            ));
        }

        sb.append("\n--- TABELA: PARCEIROS (CLIENTES) ---\n");
        sb.append("ID,NOME,STATUS\n");
        for (Partner p : parceiros) {
            sb.append(String.format("%d,%s,%s\n",
                    p.getId(),
                    sanitize(p.getName()),
                    p.getStatus()
            ));
        }

        sb.append("\n--- TABELA: EVENTOS ---\n");
        sb.append("ID,TITULO,STATUS,DATA,ID_VEICULO\n");
        for (Event e : eventos) {
            sb.append(String.format("%d,%s,%s,%s,%s\n",
                    e.getId(),
                    sanitize(e.getTitulo()),
                    e.getStatus(),
                    (e.getDataAconteceu() != null ? e.getDataAconteceu().toString() : "N/A"),
                    (e.getVehicle() != null ? e.getVehicle().getId() : "N/A")
            ));
        }

        sb.append("\n--- TABELA: PAGAMENTOS ---\n");
        sb.append("ID,VALOR,VENCIMENTO,ID_PARCEIRO\n");
        for (Payment p : pagamentos) {
            sb.append(String.format("%d,%.2f,%d,%s\n",
                    p.getId(),
                    (p.getMonthly() != null ? p.getMonthly() : 0.0),
                    p.getVencimento(),
                    (p.getPartner() != null ? p.getPartner().getId() : "N/A")
            ));
        }

        return sb.toString();
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace(",", " ").replace("\n", " ").trim();
    }

    // --- CHAMADA API ---

    private String callGeminiApi(String prompt, List<String> base64Images, double temperature, boolean jsonMode) {
        String resolvedApiKey = resolveApiKey();
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                modelName, resolvedApiKey
        );

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        parts.add(Map.of("text", prompt));

        if (base64Images != null) {
            for (String base64Image : base64Images) {
                Map<String, Object> inlineData = new HashMap<>();
                inlineData.put("mime_type", "image/png");
                inlineData.put("data", base64Image);
                parts.add(Map.of("inline_data", inlineData));
            }
        }

        requestBody.put("contents", List.of(Map.of("parts", parts)));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        if (jsonMode) {
            generationConfig.put("response_mime_type", "application/json");
        }
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return extractTextFromResponse(response.getBody());
        } catch (HttpClientErrorException ex) {
            logger.error("❌ Erro Gemini: {}", ex.getResponseBodyAsString());
            throw new RuntimeException("Erro API Gemini: " + ex.getStatusCode());
        } catch (Exception ex) {
            throw new RuntimeException("Erro interno ao chamar IA", ex);
        }
    }

    // --- UTILS ---

    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) throw new RuntimeException("IA não retornou texto.");
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText().trim();
        } catch (Exception e) {
            throw new RuntimeException("Erro parse resposta IA", e);
        }
    }

    private ExtractedDataDto parseGeminiResponse(String jsonText) throws Exception {
        String cleanJson = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();
        return objectMapper.readValue(cleanJson, ExtractedDataDto.class);
    }

    private List<String> convertPdfToBase64Images(MultipartFile pdfFile) throws IOException {
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

    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) return apiKey;
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null) return envKey;
        throw new IllegalStateException("API Key Gemini não encontrada!");
    }

    // --- PROMPTS ---

    private String buildExtractionPrompt() {
        return """
                Extraia dados da nota fiscal em JSON estrito:
                { "numeroNota": "string", "dataEmissao": "YYYY-MM-DD", "valor": "1234.50", "placa": "ABC-1234" }
                Se falhar, retorne null nos campos.
                """;
    }

    private String buildDataAnalysisPrompt(String pergunta, String dadosCsv) {
        return """
                Atue como um Analista de Dados Sênior da empresa Necsus.
                Você recebeu os dados brutos do sistema abaixo (formato CSV).
                
                Sua missão:
                1. Ler os dados das tabelas abaixo.
                2. Cruzar as informações (ex: contar veículos por status, somar pagamentos).
                3. Responder a pergunta do usuário com base APENAS nestes dados.
                
                Formato da Resposta:
                - Retorne um HTML simples (sem tags html/body, apenas div, p, table, ul, li, b).
                - Use classes Bootstrap se possível (ex: table-striped, badge bg-success).
                - Seja direto e executivo.
                - Se não encontrar a resposta nos dados, diga claramente "Não encontrei dados sobre isso".

                PERGUNTA DO USUÁRIO: "%s"

                === DADOS DO SISTEMA (CSV) ===
                %s
                """.formatted(pergunta, dadosCsv);
    }
}