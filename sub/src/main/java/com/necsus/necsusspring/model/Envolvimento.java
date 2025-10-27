package com.necsus.necsusspring.model;

public enum Envolvimento {
    CAUSADOR("CAUSADOR"),
    VITIMA("VITIMA"),
    NAO_INFORMADO("NAO INFORMADO");

    private final String descricao;

    Envolvimento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
