package com.necsus.necsusspring.model;

/**
 * Enum que define os tipos de notificação do sistema.
 * Cada tipo representa uma categoria diferente de notificação que pode ser enviada aos usuários.
 */
public enum NotificationType {

    /**
     * Notificação relacionada a eventos (criação, atualização, mudança de status)
     */
    EVENT("Evento"),

    /**
     * Notificação relacionada a demandas/tarefas
     */
    DEMAND("Demanda"),

    /**
     * Notificação relacionada a pagamentos
     */
    PAYMENT("Pagamento"),

    /**
     * Notificação relacionada a boletos
     */
    BANK_SLIP("Boleto"),

    /**
     * Notificação relacionada a comunicados
     */
    COMUNICADO("Comunicado"),

    /**
     * Notificação relacionada a parceiros/associados
     */
    PARTNER("Parceiro"),

    /**
     * Notificação relacionada a veículos
     */
    VEHICLE("Veículo"),

    /**
     * Notificação do sistema (manutenção, atualizações, etc)
     */
    SYSTEM("Sistema"),

    /**
     * Notificação de alerta/urgência
     */
    ALERT("Alerta"),

    /**
     * Notificação informativa geral
     */
    INFO("Informação");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
