package com.necsus.necsusspring.dto;

public record SaleRequest(
    String cooperativa,
    String tipoVeiculo,
    String placa,
    String marca,
    String anoModelo,
    String modelo,
    String nomeContato,
    String email,
    String celular,
    String estado,
    String cidade,
    String origemLead,
    Boolean veiculoTrabalho,
    Boolean enviarCotacao,
    String observacoes,
    Double valorVenda
) {}
