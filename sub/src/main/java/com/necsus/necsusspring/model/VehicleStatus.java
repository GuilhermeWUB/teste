package com.necsus.necsusspring.model;

public enum VehicleStatus {
    ATIVO(1, "ATIVO"),
    ATIVO_COM_PENDENCIA(2, "ATIVO COM PENDENCIA"),
    ATIVO_PLACA_ZERO(3, "ATIVO PLACA ZERO"),
    ATIVO_RASTREADOR(4, "ATIVO RASTREADOR"),
    CANCELADO(5, "CANCELADO"),
    INADIMPLENTE(6, "INADIMPLENTE"),
    INATIVO(7, "INATIVO"),
    JURIDICO(8, "JURIDICO"),
    LEAD_DE_REATIVACAO(9, "LEAD DE REATIVAÇÃO"),
    MIGRADO(10, "MIGRADO"),
    NEGADO(11, "NEGADO"),
    PENDENTE(12, "PENDENTE"),
    PERDA_TOTAL(13, "PERDA TOTAL"),
    ROUBO_FURTO(14, "ROUBO/FURTO"),
    SUBSTITUIDO(15, "SUBSTITUIDO"),
    SUSPENSO(16, "SUSPENSO"),
    VENDIDO(17, "VENDIDO");

    private final int id;
    private final String displayName;

    VehicleStatus(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static VehicleStatus fromId(int id) {
        for (VehicleStatus status : VehicleStatus.values()) {
            if (status.getId() == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid vehicle status id: " + id);
    }
}
