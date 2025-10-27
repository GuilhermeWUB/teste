package com.necsus.necsusspring.model;

public enum PartnerStatus {
    ROUBO_FURTO(1, "ROUBO/FURTO"),
    SUBSTITUIDO(2, "SUBSTITUIDO"),
    SUSPENSO(3, "SUSPENSO"),
    VENDIDO(4, "VENDIDO"),
    ATIVO(5, "ATIVO"),
    ATIVO_COM_PENDENCIA(6, "ATIVO COM PENDENCIA"),
    ATIVO_PLACA_ZERO(7, "ATIVO PLACA ZERO"),
    ATIVO_RASTREADOR(8, "ATIVO RASTREADOR"),
    CANCELADO(9, "CANCELADO"),
    INADIMPLENTE(10, "INADIMPLENTE"),
    INATIVO(11, "INATIVO"),
    JURIDICO(12, "JURIDICO"),
    LEAD_DE_REATIVACAO(13, "LEAD DE REATIVAÇÃO"),
    MIGRADO(14, "MIGRADO"),
    NEGADO(15, "NEGADO"),
    PENDENTE(16, "PENDENTE"),
    PERDA_TOTAL(17, "PERDA TOTAL");

    private final int id;
    private final String displayName;

    PartnerStatus(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PartnerStatus fromId(int id) {
        for (PartnerStatus status : PartnerStatus.values()) {
            if (status.getId() == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status id: " + id);
    }
}
