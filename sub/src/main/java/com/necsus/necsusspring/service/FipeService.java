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
     * Busca informações do veículo na API da Fipe usando o código FIPE
     * @param codigoFipe Código FIPE do veículo (formato: 001004-9)
     * @param tabelaReferencia Código da tabela FIPE de referência (opcional, usa a mais atual se não especificado)
     * @return Dados do veículo
     */
    public FipeResponseDTO buscarVeiculoPorCodigoFipe(String codigoFipe, Integer tabelaReferencia) {
        try {
            // Monta a URL completa conforme documentação
            String url = String.format("%s/cars/%s", FIPE_API_BASE_URL, codigoFipe);

            // Adiciona tabela_referencia se fornecido
            if (tabelaReferencia != null) {
                url += "?tabela_referencia=" + tabelaReferencia;
            }

            System.out.println("Buscando dados da Fipe - URL: " + url);

            // Configura os headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Faz a requisição
            ResponseEntity<FipeResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                FipeResponseDTO.class
            );

            System.out.println("Resposta da API Fipe recebida com sucesso!");
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Erro ao buscar dados da Fipe: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao buscar dados da Fipe: " + e.getMessage());
        }
    }

    /**
     * Versão simplificada que busca usando a tabela mais atual
     */
    public FipeResponseDTO buscarVeiculoPorCodigoFipe(String codigoFipe) {
        return buscarVeiculoPorCodigoFipe(codigoFipe, null);
    }
}
