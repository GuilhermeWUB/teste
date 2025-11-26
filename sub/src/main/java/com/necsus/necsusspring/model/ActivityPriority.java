package com.necsus.necsusspring.model;

public enum ActivityPriority {
    BAIXA("Baixa"),
    MEDIA("Média"),
    ALTA("Alta"),
    URGENTE("Urgente");

    private final String displayName;

    ActivityPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
