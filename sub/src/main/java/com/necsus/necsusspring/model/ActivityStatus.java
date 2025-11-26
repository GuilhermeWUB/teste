package com.necsus.necsusspring.model;

public enum ActivityStatus {
    AGENDADA("Agendada"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada"),
    REAGENDADA("Reagendada");

    private final String displayName;

    ActivityStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
