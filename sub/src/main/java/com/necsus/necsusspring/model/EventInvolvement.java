package com.necsus.necsusspring.model;

public enum EventInvolvement {
    CAUSADOR("CAUSADOR"),
    VITIMA("VITIMA"),
    NAO_INFORMADO("NAO INFORMADO");

    private final String label;

    EventInvolvement(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
