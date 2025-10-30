package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.BrasilApiFipeResponse;
import com.necsus.necsusspring.dto.FipeResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FipeService.class);
    private static final String BRASIL_API_BASE_URL = "https://brasilapi.com.br/api/fipe/preco/v1";
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter MES_REFERENCIA_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM 'de' uuuu")
            .toFormatter(PT_BR);
    private static final Comparator<BrasilApiFipeResponse> BRASIL_API_COMPARATOR = Comparator
            .comparing(FipeService::parseMesReferencia, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(BrasilApiFipeResponse::getAnoModelo, Comparator.nullsLast(Comparator.reverseOrder()));

    private final RestTemplate restTemplate;

    @Autowired
    public FipeService(RestTemplateBuilder restTemplateBuilder) {
        this(restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build());
    }

    FipeService(RestTemplate restTemplate) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate");
    }

    /**
     * Busca informações do veículo na API da Fipe usando o código FIPE
     * @param codigoFipe Código FIPE do veículo (formato: 001004-9)
     * @param tabelaReferencia Código da tabela FIPE de referência (opcional, usa a mais atual se não especificado)
     * @return Dados do veículo
     */
    public FipeResponseDTO buscarVeiculoPorCodigoFipe(String codigoFipe, Integer tabelaReferencia) {
        LOGGER.info("Buscando dados do veículo com código FIPE: {}", codigoFipe);
        return buscarDadosBrasilApi(codigoFipe, tabelaReferencia);
    }

    /**
     * Versão simplificada que busca usando a tabela mais atual
     */
    public FipeResponseDTO buscarVeiculoPorCodigoFipe(String codigoFipe) {
        return buscarVeiculoPorCodigoFipe(codigoFipe, null);
    }

    private FipeResponseDTO buscarDadosBrasilApi(String codigoFipe, Integer tabelaReferencia) {
        // Validar formato básico do código FIPE
        if (codigoFipe == null || codigoFipe.trim().isEmpty()) {
            throw new RuntimeException("O código FIPE não pode ser vazio.");
        }

        String codigoLimpo = codigoFipe.trim();

        // Verificar se o código tem um formato válido (geralmente XXX-XXXX-X ou similar)
        if (!codigoLimpo.matches("^[0-9]{6}-[0-9]$") && !codigoLimpo.matches("^[0-9]{3}-[0-9]{3,4}-[0-9]$")) {
            LOGGER.warn("Código FIPE '{}' não segue o formato esperado (000000-0 ou 000-0000-0)", codigoLimpo);
        }

        try {
            // Não usar URLEncoder pois ele codifica o hífen (-) como %2D, o que causa erro 404
            // O código FIPE contém apenas números e hífens, que são caracteres válidos em URLs
            String url = BRASIL_API_BASE_URL + "/" + codigoLimpo;
            if (tabelaReferencia != null && isLikelyModelYear(tabelaReferencia)) {
                url += "?anoModelo=" + tabelaReferencia;
            }

            LOGGER.info("Consultando BrasilAPI FIPE: {}", url);

            ResponseEntity<BrasilApiFipeResponse[]> response = restTemplate.getForEntity(URI.create(url), BrasilApiFipeResponse[].class);
            BrasilApiFipeResponse[] body = response.getBody();

            if (body == null || body.length == 0) {
                throw new RuntimeException("Código FIPE '" + codigoLimpo + "' não encontrado na base de dados da FIPE. Verifique se o código está correto e se corresponde a um veículo válido.");
            }

            BrasilApiFipeResponse selecionado = selecionarEntradaMaisRecente(body, tabelaReferencia);
            return converterBrasilApi(selecionado);
        } catch (RestClientException ex) {
            String errorMessage = ex.getMessage();
            LOGGER.error("Erro ao consultar BrasilAPI para código {}: {}", codigoLimpo, errorMessage);

            // Verificar se é erro 404 (código não encontrado)
            if (errorMessage != null && (errorMessage.contains("404") || errorMessage.contains("Not Found"))) {
                throw new RuntimeException(
                    String.format("Código FIPE '%s' não encontrado na BrasilAPI. " +
                        "Possíveis causas:\n" +
                        "• O código pode estar incorreto ou desatualizado\n" +
                        "• Verifique se o formato está correto (ex: 005340-6)\n" +
                        "• Consulte a tabela FIPE oficial para validar o código", codigoLimpo),
                    ex
                );
            }

            // Outros erros (timeout, conexão, etc)
            throw new RuntimeException("Erro ao consultar a BrasilAPI FIPE: " + errorMessage +
                ". Tente novamente mais tarde ou verifique sua conexão com a internet.", ex);
        }
    }

    private BrasilApiFipeResponse selecionarEntradaMaisRecente(BrasilApiFipeResponse[] entradas, Integer anoModelo) {
        List<BrasilApiFipeResponse> validEntries = Arrays.stream(entradas)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (validEntries.isEmpty()) {
            throw new RuntimeException("Nenhum dado válido retornado pela BrasilAPI.");
        }

        if (anoModelo != null) {
            return validEntries.stream()
                    .filter(entry -> anoModelo.equals(entry.getAnoModelo()))
                    .max(BRASIL_API_COMPARATOR)
                    .orElseGet(() -> validEntries.stream()
                            .max(BRASIL_API_COMPARATOR)
                            .orElse(validEntries.get(0)));
        }

        return validEntries.stream()
                .max(BRASIL_API_COMPARATOR)
                .orElse(validEntries.get(0));
    }

    private FipeResponseDTO converterBrasilApi(BrasilApiFipeResponse origem) {
        FipeResponseDTO dto = new FipeResponseDTO();
        dto.setBrand(origem.getMarca());
        dto.setCodeFipe(origem.getCodigoFipe());
        dto.setFuel(origem.getCombustivel());
        dto.setFuelAcronym(origem.getSiglaCombustivel());
        dto.setModel(origem.getModelo());
        dto.setModelYear(origem.getAnoModelo());
        dto.setPrice(origem.getValor());
        dto.setReferenceMonth(origem.getMesReferencia());
        dto.setVehicleType(origem.getTipoVeiculo());
        return dto;
    }

    private static boolean isLikelyModelYear(Integer value) {
        int currentYear = Year.now().plusYears(1).getValue();
        return value >= 1900 && value <= currentYear;
    }

    private static YearMonth parseMesReferencia(BrasilApiFipeResponse response) {
        String mesReferencia = response.getMesReferencia();
        if (mesReferencia == null || mesReferencia.isBlank()) {
            return null;
        }
        try {
            return YearMonth.from(MES_REFERENCIA_FORMATTER.parse(mesReferencia));
        } catch (DateTimeParseException ex) {
            LOGGER.debug("Não foi possível interpretar o mês de referência '{}'", mesReferencia, ex);
            return null;
        }
    }
}
