package com.necsus.necsusspring.model;

public enum ActivityType {
    LIGACAO("Ligação"),
    EMAIL("E-mail"),
    REUNIAO("Reunião"),
    VISITA("Visita"),
    FOLLOW_UP("Follow-up"),
    APRESENTACAO("Apresentação"),
    NEGOCIACAO("Negociação"),
    VISTORIA("Vistoria"),
    OUTRO("Outro");

    private final String displayName;

    ActivityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
