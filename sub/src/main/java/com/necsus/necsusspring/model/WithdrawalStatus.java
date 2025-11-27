package com.necsus.necsusspring.model;

import lombok.Getter;

@Getter
public enum WithdrawalStatus {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    REJEITADO("Rejeitado"),
    CONCLUIDO("Concluído");

    private final String descricao;

    WithdrawalStatus(String descricao) {
        this.descricao = descricao;
    }

    public static WithdrawalStatus fromString(String status) {
        for (WithdrawalStatus ws : WithdrawalStatus.values()) {
            if (ws.name().equalsIgnoreCase(status)) {
                return ws;
            }
        }
        return PENDENTE;
    }
}
