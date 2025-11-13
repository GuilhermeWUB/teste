package com.necsus.necsusspring.model;

/**
 * Enum que representa os possíveis status de um processo jurídico no Kanban.
 */
public enum LegalProcessStatus {
    RASTREADOR_EM_ABERTO("Rastreador - Em Aberto"),
    RASTREADOR_EM_CONTATO("Rastreador - Em Contato"),
    RASTREADOR_ACORDO_ASSINADO("Rastreador - Acordo Assinado"),
    RASTREADOR_DEVOLVIDO("Rastreador Devolvido"),
    RASTREADOR_REATIVACAO("Rastreador - Reativação"),

    FIDELIDADE_EM_ABERTO("Fidelidade - Em Aberto"),
    FIDELIDADE_EM_CONTATO("Fidelidade - Em Contato"),
    FIDELIDADE_ACORDO_ASSINADO("Fidelidade - Acordo Assinado"),
    FIDELIDADE_REATIVACAO("Fidelidade - Reativação"),

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
