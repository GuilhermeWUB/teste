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
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.regex.Pattern;

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
    private final JdbcTemplate jdbcTemplate;

    public GeminiService(JdbcTemplate jdbcTemplate) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.jdbcTemplate = jdbcTemplate;
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

    /**
     * Gera relatório dinâmico baseado em uma pergunta em linguagem natural.
     * Converte a pergunta em SQL usando Gemini AI e executa no banco de dados.
     *
     * IMPORTANTE: Este método executa SQL dinâmico. Para produção, recomenda-se usar
     * um usuário de banco de dados com permissões SOMENTE LEITURA (apenas SELECT).
     *
     * @param pergunta A pergunta em linguagem natural (ex: "Quantos veículos ativos temos?")
     * @return Lista de mapas contendo os dados retornados pela query
     * @throws Exception Se houver erro na geração ou execução do SQL
     */
    public List<Map<String, Object>> gerarRelatorioPorTexto(String pergunta) throws Exception {
        logger.info("Gerando relatório para pergunta: {}", pergunta);

        // 1. Construir o prompt com esquema do banco
        String prompt = buildSqlGenerationPrompt(pergunta);

        // 2. Chamar Gemini para gerar SQL
        String sql = callGeminiForSql(prompt);

        // 3. Sanitizar e validar SQL
        sql = sanitizeSql(sql);

        // 4. Executar SQL
        logger.info("Executando SQL gerado: {}", sql);
        List<Map<String, Object>> resultados = jdbcTemplate.queryForList(sql);

        logger.info("Relatório gerado com sucesso. {} linhas retornadas", resultados.size());
        return resultados;
    }

    /**
     * Constrói o prompt para geração de SQL com esquema do banco
     */
    private String buildSqlGenerationPrompt(String pergunta) {
        return """
                Você é um especialista em SQL e banco de dados PostgreSQL.

                Baseado no seguinte esquema de banco de dados, gere uma query SQL para responder a pergunta do usuário.

                ESQUEMA DO BANCO DE DADOS:

                1. Tabela: app_users
                   - id (BIGINT, PRIMARY KEY)
                   - full_name (VARCHAR)
                   - username (VARCHAR, UNIQUE)
                   - email (VARCHAR, UNIQUE)
                   - role (VARCHAR) - Tipos: ADMIN, USER, etc.
                   - created_at (TIMESTAMP)

                2. Tabela: partner
                   - id (BIGINT, PRIMARY KEY)
                   - name (VARCHAR)
                   - date_born (DATE)
                   - email (VARCHAR)
                   - cpf (VARCHAR)
                   - phone (VARCHAR)
                   - cell (VARCHAR)
                   - rg (VARCHAR)
                   - status (VARCHAR) - Tipos: ACTIVE, INACTIVE, etc.

                3. Tabela: vehicle
                   - id (BIGINT, PRIMARY KEY)
                   - maker (VARCHAR) - Montadora
                   - type_vehicle (VARCHAR)
                   - plaque (VARCHAR) - Placa
                   - partners_id (BIGINT) - FK para partner
                   - model (VARCHAR)
                   - status (INTEGER)
                   - vehicle_status (VARCHAR) - Tipos: ACTIVE, INACTIVE, etc.

                4. Tabela: event
                   - id (BIGINT, PRIMARY KEY)
                   - titulo (VARCHAR)
                   - descricao (TEXT)
                   - status (VARCHAR) - Tipos: PENDING, COMPLETED, etc.
                   - prioridade (VARCHAR) - ALTA, MEDIA, BAIXA
                   - motivo (VARCHAR)
                   - envolvimento (VARCHAR)
                   - data_aconteceu (DATE)
                   - data_comunicacao (DATE)

                5. Tabela: info_payment (pagamentos)
                   - id (BIGINT, PRIMARY KEY)
                   - vehicle_id (BIGINT) - FK para vehicle
                   - partners_id (BIGINT) - FK para partner
                   - monthly (DECIMAL) - Mensalidade
                   - vencimento (INTEGER) - Dia do vencimento (1-31)
                   - date_create (TIMESTAMP)

                6. Tabela: legal_processes
                   - id (BIGINT, PRIMARY KEY)
                   - autor (VARCHAR)
                   - reu (VARCHAR)
                   - materia (VARCHAR)
                   - numero_processo (VARCHAR, UNIQUE)
                   - valor_causa (DECIMAL)
                   - pedidos (TEXT)
                   - status (VARCHAR) - EM_ABERTO_7_0, etc.
                   - process_type (VARCHAR) - TERCEIROS, etc.
                   - source_event_id (BIGINT)

                PERGUNTA DO USUÁRIO:
                """ + pergunta + """

                INSTRUÇÕES CRÍTICAS:
                - Retorne APENAS o SQL puro, sem markdown, sem explicações, sem ```sql
                - Use SOMENTE SELECT (não use INSERT, UPDATE, DELETE, DROP, ALTER, etc.)
                - Use nomes de tabelas e colunas exatamente como no esquema
                - Para contagens, use COUNT(*)
                - Para valores monetários, use SUM() e formate se necessário
                - Adicione ORDER BY quando relevante
                - Limite resultados com LIMIT quando apropriado
                - Use JOINs quando precisar relacionar tabelas

                Retorne apenas o SQL:
                """;
    }

    /**
     * Chama a API do Gemini para gerar SQL
     */
    private String callGeminiForSql(String prompt) throws Exception {
        String url = String.format(
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
            modelName,
            apiKey
        );

        // Preparar o body da requisição
        Map<String, Object> requestBody = new HashMap<>();

        // Construir as parts (apenas texto)
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);

        // Construir contents
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);
        contents.add(content);

        requestBody.put("contents", contents);

        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        logger.info("Chamando Gemini API para gerar SQL...");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Erro ao chamar Gemini API: " + response.getStatusCode());
        }

        // Extrair o SQL da resposta
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode candidates = root.path("candidates");

        if (candidates.isEmpty()) {
            throw new RuntimeException("Nenhum SQL gerado pela API");
        }

        String sql = candidates.get(0)
            .path("content")
            .path("parts")
            .get(0)
            .path("text")
            .asText()
            .trim();

        logger.debug("SQL bruto gerado: {}", sql);
        return sql;
    }

    /**
     * Sanitiza e valida o SQL gerado para prevenir SQL injection e comandos perigosos.
     *
     * SEGURANÇA: Esta validação é uma camada de proteção adicional, mas NÃO substitui
     * a necessidade de usar um usuário de banco com permissões limitadas.
     */
    private String sanitizeSql(String sql) {
        // Remover possíveis marcadores de markdown
        sql = sql.replaceAll("```sql\\s*", "")
                 .replaceAll("```\\s*", "")
                 .trim();

        // Normalizar espaços
        String sqlUpper = sql.toUpperCase().replaceAll("\\s+", " ");

        // Verificar se começa com SELECT
        if (!sqlUpper.startsWith("SELECT")) {
            throw new SecurityException("SQL deve começar com SELECT. SQL fornecido: " + sql);
        }

        // Lista de comandos perigosos proibidos
        String[] comandosProibidos = {
            "DELETE", "UPDATE", "DROP", "ALTER", "CREATE", "INSERT",
            "TRUNCATE", "GRANT", "REVOKE", "EXEC", "EXECUTE", "--", "/*", "*/"
        };

        for (String comando : comandosProibidos) {
            if (sqlUpper.contains(comando)) {
                throw new SecurityException(
                    "SQL contém comando proibido: " + comando + ". Apenas SELECT é permitido."
                );
            }
        }

        // Verificar por ponto-e-vírgula múltiplos (tentativa de injetar múltiplos comandos)
        if (sql.indexOf(';') != sql.lastIndexOf(';') && sql.indexOf(';') != -1) {
            throw new SecurityException("SQL não pode conter múltiplos comandos (múltiplos ;)");
        }

        // Remover ponto-e-vírgula final se existir
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }

        logger.info("SQL validado e sanitizado com sucesso");
        return sql;
    }
}
