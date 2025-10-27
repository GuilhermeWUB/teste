package com.necsus.necsusspring.model;

public enum Motivo {
    COLISAO("COLISÃO"),
    ROUBO("ROUBO"),
    FURTO("FURTO"),
    NAO_INFORMADO("NAO INFORMADO"),
    VENTO_ALAGAMENTO_GRANIZO_ETC("VENTO, ALAGAMENTO, GRANIZO, ETC"),
    VIDROS_E_PARA_BRISA("VIDROS E PARA-BRISA"),
    FAROIS_E_LANTERNAS("FAROIS E LANTERNAS"),
    RETROVISORES("RETROVISORES"),
    COBRANCA_FIDELIDADE("COBRANÇA FIDELIDADE");

    private final String descricao;

    Motivo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
