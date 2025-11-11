package com.necsus.necsusspring.model;

/**
 * Enum que define os status possíveis de uma notificação.
 * Permite rastrear o estado de leitura e arquivamento das notificações.
 */
public enum NotificationStatus {

    /**
     * Notificação não lida - estado inicial
     */
    UNREAD("Não Lida"),

    /**
     * Notificação já visualizada pelo usuário
     */
    READ("Lida"),

    /**
     * Notificação arquivada pelo usuário
     */
    ARCHIVED("Arquivada");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
