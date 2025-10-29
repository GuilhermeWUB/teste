package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.BrasilApiFipeResponse;
import com.necsus.necsusspring.dto.FipeResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private static final String FIPE_API_BASE_URL = "https://fipe.parallelum.com.br/api/v2";
    private static final String BRASIL_API_BASE_URL = "https://brasilapi.com.br/api/fipe/preco/v2";
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter MES_REFERENCIA_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM 'de' uuuu")
            .toFormatter(PT_BR);
    private static final Comparator<BrasilApiFipeResponse> BRASIL_API_COMPARATOR = Comparator
            .comparing(FipeService::parseMesReferencia, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(BrasilApiFipeResponse::getAnoModelo, Comparator.nullsLast(Comparator.reverseOrder()));

    private final RestTemplate restTemplate;

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
        RuntimeException lastException = null;

        try {
            return buscarDadosBrasilApi(codigoFipe, tabelaReferencia);
        } catch (RuntimeException brasilApiError) {
            lastException = brasilApiError;
            LOGGER.warn("Falha ao consultar BrasilAPI para o código FIPE {}: {}", codigoFipe, brasilApiError.getMessage());
        }

        try {
            return buscarDadosFipeOficial(codigoFipe, tabelaReferencia);
        } catch (RuntimeException fipeApiError) {
            if (lastException != null) {
                fipeApiError.addSuppressed(lastException);
            }
            throw fipeApiError;
        }
    }

    /**
     * Versão simplificada que busca usando a tabela mais atual
     */
    public FipeResponseDTO buscarVeiculoPorCodigoFipe(String codigoFipe) {
        return buscarVeiculoPorCodigoFipe(codigoFipe, null);
    }

    private FipeResponseDTO buscarDadosBrasilApi(String codigoFipe, Integer tabelaReferencia) {
        try {
            String encodedCode = URLEncoder.encode(codigoFipe, StandardCharsets.UTF_8);
            String url = BRASIL_API_BASE_URL + "/" + encodedCode;
            if (tabelaReferencia != null && isLikelyModelYear(tabelaReferencia)) {
                url += "?anoModelo=" + tabelaReferencia;
            }

            LOGGER.info("Consultando BrasilAPI FIPE: {}", url);

            ResponseEntity<BrasilApiFipeResponse[]> response = restTemplate.getForEntity(URI.create(url), BrasilApiFipeResponse[].class);
            BrasilApiFipeResponse[] body = response.getBody();

            if (body == null || body.length == 0) {
                throw new RuntimeException("Nenhum dado retornado pela BrasilAPI para o código informado.");
            }

            BrasilApiFipeResponse selecionado = selecionarEntradaMaisRecente(body, tabelaReferencia);
            return converterBrasilApi(selecionado);
        } catch (RestClientException ex) {
            throw new RuntimeException("Erro ao consultar a BrasilAPI: " + ex.getMessage(), ex);
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

    private FipeResponseDTO buscarDadosFipeOficial(String codigoFipe, Integer tabelaReferencia) {
        try {
            String encodedCode = URLEncoder.encode(codigoFipe, StandardCharsets.UTF_8);
            StringBuilder urlBuilder = new StringBuilder(FIPE_API_BASE_URL)
                    .append("/cars/")
                    .append(encodedCode);

            if (tabelaReferencia != null) {
                urlBuilder.append("?tabela_referencia=").append(tabelaReferencia);
            }

            String url = urlBuilder.toString();
            LOGGER.info("Consultando API FIPE oficial: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<FipeResponseDTO> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    entity,
                    FipeResponseDTO.class
            );

            FipeResponseDTO body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Resposta vazia da API FIPE oficial.");
            }

            return body;
        } catch (RestClientException ex) {
            throw new RuntimeException("Erro ao consultar a API FIPE oficial: " + ex.getMessage(), ex);
        }
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
