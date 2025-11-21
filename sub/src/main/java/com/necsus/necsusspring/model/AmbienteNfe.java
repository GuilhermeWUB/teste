package com.necsus.necsusspring.model;

/**
 * Enum para definir o ambiente de comunicação com a SEFAZ
 */
public enum AmbienteNfe {
    HOMOLOGACAO("Homologação"),
    PRODUCAO("Produção");

    private final String descricao;

    AmbienteNfe(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
