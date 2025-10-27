package com.necsus.necsusspring.model;

public enum EventReason {
    COLISAO("COLISÃO"),
    ROUBO("ROUBO"),
    FURTO("FURTO"),
    NAO_INFORMADO("NÃO INFORMADO"),
    VENTO_ALAGAMENTO_GRANIZO_ETC("VENTO, ALAGAMENTO, GRANIZO, ETC"),
    VIDROS_E_LANTERNAS("VIDROS E LANTERNAS"),
    FAROIS_E_PARA_BRISA("FARÓIS E PARA-BRISA"),
    RETROVISORES("RETROVISORES"),
    COBRANCA_FIDELIDADE("COBRANÇA FIDELIDADE");

    private final String label;

    EventReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
