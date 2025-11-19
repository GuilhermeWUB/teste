package com.necsus.necsusspring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.necsus.necsusspring.dto.ExtractedDataDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class GeminiExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiExtractionService.class);

    @Value("${gemini.project-id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    @Value("${gemini.model-name}")
    private String modelName;

    public ExtractedDataDto extractDataFromPdf(MultipartFile pdfFile) throws IOException {
        logger.info("Iniciando extração de dados do PDF: {}", pdfFile.getOriginalFilename());

        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            List<String> base64Images = convertPdfPagesToBase64Images(document);
            String jsonResponse = callGeminiApi(base64Images);
            return parseJsonToDto(jsonResponse);
        } catch (Exception e) {
            logger.error("Falha ao extrair dados do PDF.", e);
            throw new IOException("Erro ao processar o PDF com a API do Gemini.", e);
        }
    }

    private List<String> convertPdfPagesToBase64Images(PDDocument document) throws IOException {
        List<String> base64Images = new ArrayList<>();
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        int pageCount = Math.min(document.getNumberOfPages(), 5); // Limita a 5 páginas

        for (int i = 0; i < pageCount; i++) {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(i, 300); // 300 DPI para boa qualidade
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bim, "jpeg", baos);
            byte[] imageBytes = baos.toByteArray();
            base64Images.add(Base64.getEncoder().encodeToString(imageBytes));
        }
        logger.info("{} páginas do PDF foram convertidas para imagem.", base64Images.size());
        return base64Images;
    }

    private String callGeminiApi(List<String> base64Images) throws IOException {
        // O SDK do Java para Vertex AI é mais complexo para inicialização de API Key direta.
        // A abordagem mais comum é usar a CLI `gcloud auth application-default login` no ambiente
        // de desenvolvimento. O código abaixo representa a lógica de chamada, assumindo
        // que a autenticação foi configurada.
        try (VertexAI vertexAi = new VertexAI(projectId, location)) {
            GenerativeModel model = new GenerativeModel(modelName, vertexAi);

            // Monta o prompt
            String prompt = "Analise as imagens da nota fiscal em anexo e extraia as seguintes informações:" +
                    " - Número da Nota" +
                    " - Data de Emissão (no formato YYYY-MM-DD)" +
                    " - Valor Total (use ponto como separador decimal, sem separador de milhar)" +
                    " - Placa do Veículo (se houver)" +
                    "Retorne a resposta EXCLUSIVAMENTE em formato JSON, como no exemplo: " +
                    "{\"numeroNota\": \"12345\", \"dataEmissao\": \"2023-10-27\", \"valor\": \"150.75\", \"placa\": \"ABC1234\"}";

            List<com.google.cloud.vertexai.api.Part> parts = new ArrayList<>();
            parts.add(PartMaker.fromMimeTypeAndData("text/plain", prompt));
            for (String base64Image : base64Images) {
                parts.add(PartMaker.fromMimeTypeAndData("image/jpeg", Base64.getDecoder().decode(base64Image)));
            }

            // Configurações de segurança para evitar bloqueios
            GenerationConfig generationConfig = GenerationConfig.newBuilder()
                    .setMaxOutputTokens(2048)
                    .setTemperature(0.2f)
                    .setTopP(0.95f)
                    .build();

            List<SafetySetting> safetySettings = new ArrayList<>();
            safetySettings.add(SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build());
            safetySettings.add(SafetySetting.newBuilder().setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT).setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH).build());


            logger.info("Enviando requisição para a API do Gemini...");
            GenerateContentResponse response = model.generateContent(ContentMaker.fromMultiModalData(parts), generationConfig, safetySettings, null);

            String jsonText = ResponseHandler.getText(response).trim();
            // Limpa a resposta para garantir que seja apenas o JSON
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7, jsonText.length() - 3).trim();
            }
            logger.info("Resposta JSON recebida da API do Gemini: {}", jsonText);
            return jsonText;
        }
    }

    private ExtractedDataDto parseJsonToDto(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, ExtractedDataDto.class);
        } catch (IOException e) {
            logger.error("Falha ao desserializar JSON da API do Gemini: {}", json, e);
            // Retorna um DTO vazio ou lança uma exceção específica
            return new ExtractedDataDto();
        }
    }
}
