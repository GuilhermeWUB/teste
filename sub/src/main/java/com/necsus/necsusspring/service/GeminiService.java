package com.necsus.necsusspring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.necsus.necsusspring.dto.ExtractedDataDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço para extração de dados de notas fiscais usando Gemini 2.5 Flash API
 */
@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final int MAX_PAGES = 5;
    private static final int DPI = 200; // Aumentado para melhor qualidade de leitura

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash-exp}")
    private String modelName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Extrai dados de nota fiscal de um arquivo PDF usando Gemini API
     */
    public ExtractedDataDto extractDataFromPdf(MultipartFile pdfFile) throws Exception {
        logger.info("Iniciando extração de dados do PDF: {}", pdfFile.getOriginalFilename());

        // 1. Converter páginas do PDF em imagens base64
        List<String> base64Images = convertPdfToBase64Images(pdfFile);

        if (base64Images.isEmpty()) {
            throw new RuntimeException("Não foi possível converter o PDF em imagens");
        }

        // 2. Preparar o prompt para o Gemini
        String prompt = buildExtractionPrompt();

        // 3. Chamar a API do Gemini
        String jsonResponse = callGeminiApi(prompt, base64Images);

        // 4. Parsear a resposta
        ExtractedDataDto extractedData = parseGeminiResponse(jsonResponse);

        logger.info("Dados extraídos com sucesso: {}", extractedData);
        return extractedData;
    }

    /**
     * Converte as primeiras páginas do PDF em imagens base64
     */
    private List<String> convertPdfToBase64Images(MultipartFile pdfFile) throws IOException {
        List<String> base64Images = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pagesToProcess = Math.min(document.getNumberOfPages(), MAX_PAGES);

            for (int pageIndex = 0; pageIndex < pagesToProcess; pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, DPI);
                String base64Image = imageToBase64(image);
                base64Images.add(base64Image);
                logger.debug("Página {} convertida para base64", pageIndex + 1);
            }
        }

        return base64Images;
    }

    /**
     * Converte BufferedImage para base64
     */
    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Constrói o prompt para extração de dados
     */
    private String buildExtractionPrompt() {
        return """
                Você é um assistente especializado em extrair dados de notas fiscais brasileiras (NF-e, NFS-e, Danfe).

                Analise CUIDADOSAMENTE a(s) imagem(ns) da nota fiscal e extraia as seguintes informações:

                1. **numeroNota**: O número da nota fiscal
                   - Procure por: "Nº", "N°", "NÚMERO", "NF-e Nº", "NOTA FISCAL Nº"
                   - Retorne apenas os números (ex: "12345")

                2. **dataEmissao**: A data de emissão
                   - Procure por: "DATA DE EMISSÃO", "EMISSÃO", "DT. EMISSÃO"
                   - Converta para formato YYYY-MM-DD (ex: "2024-01-15")

                3. **valor**: O VALOR TOTAL da nota fiscal
                   - ATENÇÃO: Este é o campo mais importante!
                   - Procure por textos como:
                     * "VALOR TOTAL DA NOTA"
                     * "VALOR TOTAL"
                     * "TOTAL DA NOTA"
                     * "VLR TOTAL"
                     * "V. TOTAL"
                   - O valor numérico geralmente está ABAIXO ou AO LADO do texto descritivo
                   - O texto descritivo pode estar em FONTE PEQUENA
                   - Procure o maior valor em destaque no documento
                   - Exemplos: R$ 1.250,50 → retorne "1250.50"
                   - Use PONTO como separador decimal, SEM símbolo de moeda

                4. **placa**: Placa do veículo (se houver)
                   - Procure por: "PLACA", "VEÍCULO"
                   - Formatos: ABC-1234 ou ABC1D23

                INSTRUÇÕES CRÍTICAS:
                - Analise TODO o documento, incluindo áreas com texto pequeno
                - Para o VALOR: se houver um texto pequeno como "VALOR TOTAL DA NOTA", o número grande próximo é o valor
                - Se não encontrar algum campo após análise completa, retorne null
                - Retorne APENAS um JSON válido, sem explicações

                Formato de resposta:
                {
                  "numeroNota": "12345",
                  "dataEmissao": "2024-01-15",
                  "valor": "1250.50",
                  "placa": "ABC-1234"
                }
                """;
    }

    /**
     * Chama a API do Gemini para extrair dados
     */
    private String callGeminiApi(String prompt, List<String> base64Images) throws Exception {
        String url = String.format(
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
            modelName,
            apiKey
        );

        // Preparar o body da requisição
        Map<String, Object> requestBody = new HashMap<>();

        // Construir as parts (texto + imagens)
        List<Map<String, Object>> parts = new ArrayList<>();

        // Adicionar o prompt
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);

        // Adicionar as imagens
        for (String base64Image : base64Images) {
            Map<String, Object> imagePart = new HashMap<>();
            Map<String, String> inlineData = new HashMap<>();
            inlineData.put("mime_type", "image/png");
            inlineData.put("data", base64Image);
            imagePart.put("inline_data", inlineData);
            parts.add(imagePart);
        }

        // Construir contents
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);
        contents.add(content);

        requestBody.put("contents", contents);

        // Configuração de geração para retornar JSON
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        requestBody.put("generationConfig", generationConfig);

        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        logger.info("Chamando Gemini API...");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Erro ao chamar Gemini API: " + response.getStatusCode());
        }

        return response.getBody();
    }

    /**
     * Parseia a resposta da API do Gemini
     */
    private ExtractedDataDto parseGeminiResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);

        // A resposta vem no formato: candidates[0].content.parts[0].text
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            throw new RuntimeException("Nenhum candidato retornado pela API");
        }

        JsonNode firstCandidate = candidates.get(0);
        JsonNode content = firstCandidate.path("content");
        JsonNode parts = content.path("parts");

        if (parts.isEmpty()) {
            throw new RuntimeException("Nenhuma parte retornada no conteúdo");
        }

        String textContent = parts.get(0).path("text").asText();

        // O textContent já deve ser um JSON com os dados extraídos
        logger.debug("Resposta do Gemini: {}", textContent);

        return objectMapper.readValue(textContent, ExtractedDataDto.class);
    }
}
