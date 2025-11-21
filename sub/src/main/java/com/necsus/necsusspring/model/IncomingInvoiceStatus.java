package com.necsus.necsusspring.model;

/**
 * Enum para definir o status de uma nota fiscal de entrada na caixa de entrada
 */
public enum IncomingInvoiceStatus {
    PENDENTE("Pendente - Aguardando processamento"),
    PROCESSADA("Processada - Convertida em conta a pagar"),
    IGNORADA("Ignorada - Descartada pelo usuário");

    private final String descricao;

    IncomingInvoiceStatus(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
