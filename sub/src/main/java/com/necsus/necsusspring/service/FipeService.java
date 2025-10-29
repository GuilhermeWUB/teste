package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.FipeResponseDTO;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FipeService {

    private static final String FIPE_API_BASE_URL = "https://fipe.parallelum.com.br/api/v2";
    private final RestTemplate restTemplate;

    public FipeService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Busca informações do veículo na API da Fipe
     * @param codigoFipe Código Fipe do veículo (ex: 005340-6)
     * @param ano Ano do modelo (ex: 2014-3)
     * @param referencia Referência da tabela (ex: 278)
     * @param codigoCarro Código do carro na API (ex: 004278-1)
     * @return Dados do veículo
     */
    public FipeResponseDTO buscarVeiculoPorCodigo(String codigoFipe, String ano, String referencia, String codigoCarro) {
        try {
            // Monta a URL completa
            String url = String.format("%s/cars/%s/years/%s?reference=%s",
                FIPE_API_BASE_URL, codigoCarro, ano, referencia);

            // Configura os headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("accept", "application/json");
            // Nota: X-Subscription-Token pode ser necessário dependendo do plano da API
            // headers.set("X-Subscription-Token", "YOUR_TOKEN_HERE");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Faz a requisição
            ResponseEntity<FipeResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                FipeResponseDTO.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar dados da Fipe: " + e.getMessage());
        }
    }

    /**
     * Versão simplificada que tenta buscar apenas com código e ano
     * Útil se o código do carro for igual ao código Fipe
     */
    public FipeResponseDTO buscarVeiculoSimplificado(String codigoFipe, String ano) {
        try {
            // Tenta usar o código Fipe como código do carro
            // Nota: isso pode não funcionar em todos os casos, dependendo da API
            String url = String.format("%s/cars/%s/years/%s",
                FIPE_API_BASE_URL, codigoFipe, ano);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<FipeResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                FipeResponseDTO.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar dados da Fipe: " + e.getMessage());
        }
    }
}
