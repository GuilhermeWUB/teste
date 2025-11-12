package com.necsus.necsusspring.model;

/**
 * Enum que representa os possíveis status de um processo jurídico no Kanban.
 */
public enum LegalProcessStatus {
    EM_ABERTO_7_0("Em Aberto 7.0"),
    EM_CONTATO_7_1("Em Contato 7.1"),
    PROCESSO_JUDICIAL_7_2("Processo Judicial 7.2"),
    ACORDO_ASSINADO_7_3("Acordo Assinado 7.3");

    private final String displayName;

    LegalProcessStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
