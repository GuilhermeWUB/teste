package com.necsus.necsusspring.model;

public enum SaleStatus {
    // Status para funil de filiação
    COTACOES_RECEBIDAS("Cotações recebidas"),
    EM_NEGOCIACAO("Em negociação"),
    VISTORIAS("Vistorias"),
    LIBERADAS_PARA_CADASTRO("Liberadas para cadastro"),
    FILIACAO_CONCRETIZADAS("Filiação concretizadas");

    private final String displayName;

    SaleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
