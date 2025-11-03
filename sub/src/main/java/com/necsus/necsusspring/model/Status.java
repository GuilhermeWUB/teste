package com.necsus.necsusspring.model;

public enum Status {
    A_FAZER("A Fazer"),
    EM_ANDAMENTO("Em Andamento"),
    AGUARDANDO("Aguardando"),
    CONCLUIDO("Concluído");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
