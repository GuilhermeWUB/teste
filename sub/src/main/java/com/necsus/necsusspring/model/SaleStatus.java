package com.necsus.necsusspring.model;

public enum SaleStatus {
    // Status para vendas/negociações
    NOVO_LEAD("Novo Lead"),
    CONTATO_INICIAL("Contato Inicial"),
    PROPOSTA_ENVIADA("Proposta Enviada"),
    NEGOCIACAO("Negociação"),
    FECHADO("Fechado"),
    PERDIDO("Perdido");

    private final String displayName;

    SaleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
