package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.FipeResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.http.HttpStatus;

class FipeServiceTest {

    private MockRestServiceServer mockServer;
    private FipeService fipeService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        mockServer = MockRestServiceServer.createServer(restTemplate);
        fipeService = new FipeService(restTemplate);
    }

    @Test
    void deveRetornarDadosDaBrasilApiQuandoDisponivel() {
        String codigoFipe = "001004-9";
        String brasilApiResponse = """
                [
                  {
                    "valor": "R$ 52.300,00",
                    "marca": "Chevrolet",
                    "modelo": "Onix 1.0",
                    "anoModelo": 2022,
                    "combustivel": "Gasolina",
                    "siglaCombustivel": "G",
                    "codigoFipe": "001004-9",
                    "mesReferencia": "janeiro de 2024",
                    "tipoVeiculo": 1
                  },
                  {
                    "valor": "R$ 53.100,00",
                    "marca": "Chevrolet",
                    "modelo": "Onix 1.0",
                    "anoModelo": 2023,
                    "combustivel": "Gasolina",
                    "siglaCombustivel": "G",
                    "codigoFipe": "001004-9",
                    "mesReferencia": "março de 2024",
                    "tipoVeiculo": 1
                  }
                ]
                """;

        mockServer.expect(requestTo("https://brasilapi.com.br/api/fipe/preco/v1/" + codigoFipe))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(brasilApiResponse, MediaType.APPLICATION_JSON));

        FipeResponseDTO resultado = fipeService.buscarVeiculoPorCodigoFipe(codigoFipe, null);

        mockServer.verify();

        assertThat(resultado).isNotNull();
        assertThat(resultado.getBrand()).isEqualTo("Chevrolet");
        assertThat(resultado.getModelYear()).isEqualTo(2023);
        assertThat(resultado.getPrice()).isEqualTo("R$ 53.100,00");
        assertThat(resultado.getVehicleType()).isEqualTo(1);
    }

    @Test
    void lancaExcecaoQuandoBrasilApiFalhar() {
        String codigoFipe = "001004-9";
        DefaultResponseCreator brasilApiErro = withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"not found\"}");

        mockServer.expect(requestTo("https://brasilapi.com.br/api/fipe/preco/v1/" + codigoFipe))
                .andExpect(method(HttpMethod.GET))
                .andRespond(brasilApiErro);

        assertThatThrownBy(() -> fipeService.buscarVeiculoPorCodigoFipe(codigoFipe, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Código FIPE '" + codigoFipe + "' não encontrado na BrasilAPI");

        mockServer.verify();
    }

    @Test
    void naoEnviaTabelaReferenciaInvalidaParaBrasilApi() {
        String codigoFipe = "001004-9";
        String brasilApiResponse = """
                [
                  {
                    "valor": "R$ 48.000,00",
                    "marca": "Ford",
                    "modelo": "Fiesta",
                    "anoModelo": 2018,
                    "combustivel": "Gasolina",
                    "siglaCombustivel": "G",
                    "codigoFipe": "001004-9",
                    "mesReferencia": "dezembro de 2023",
                    "tipoVeiculo": 1
                  }
                ]
                """;

        mockServer.expect(requestTo("https://brasilapi.com.br/api/fipe/preco/v1/" + codigoFipe))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(brasilApiResponse, MediaType.APPLICATION_JSON));

        FipeResponseDTO resultado = fipeService.buscarVeiculoPorCodigoFipe(codigoFipe, 305);

        mockServer.verify();

        assertThat(resultado).isNotNull();
        assertThat(resultado.getBrand()).isEqualTo("Ford");
        assertThat(resultado.getModelYear()).isEqualTo(2018);
        assertThat(resultado.getPrice()).isEqualTo("R$ 48.000,00");
    }
}
