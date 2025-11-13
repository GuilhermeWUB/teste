package com.necsus.necsusspring.model;

/**
 * Enum que representa os tipos de cobrança disponíveis no Kanban de processos jurídicos.
 */
public enum LegalProcessType {
    RASTREADOR("Rastreador"),
    FIDELIDADE("Fidelidade"),
    TERCEIROS("Terceiros");

    private final String displayName;

    LegalProcessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
