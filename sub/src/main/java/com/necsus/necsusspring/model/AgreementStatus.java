package com.necsus.necsusspring.model;

public enum AgreementStatus {
    // Status para acordos a pagar
    PENDENTE("Pendente"),
    EM_NEGOCIACAO("Em Negociacao"),
    APROVADO("Aprovado"),
    PAGO("Pago"),
    CANCELADO("Cancelado"),
    VENCIDO("Vencido");

    private final String displayName;

    AgreementStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
