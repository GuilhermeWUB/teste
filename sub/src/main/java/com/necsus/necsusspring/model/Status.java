package com.necsus.necsusspring.model;

public enum Status {
    // Fase 1: Comunicação
    COMUNICADO("1.0", "Comunicado", 1, "Comunicação"),
    ABERTO("1.1", "Aberto", 1, "Comunicação"),

    // Fase 2: Análise
    VISTORIA("2.0", "Vistoria", 2, "Análise"),
    ANALISE("2.1", "Análise", 2, "Análise"),
    SINDICANCIA("2.2", "Sindicância", 2, "Análise"),
    DESISTENCIA("2.8", "Desistência", 2, "Análise"),

    // Fase 3: Negociação
    ORCAMENTO("3.0", "Orçamento", 3, "Negociação"),
    COTA_PARTICIPACAO("3.1", "Cota de Participação", 3, "Negociação"),
    ACORDO_ANDAMENTO("3.2", "Acordo em Andamento", 3, "Negociação"),

    // Fase 4: Execução
    COMPRA("4.0", "Compra", 4, "Execução"),
    AGENDADO("4.1", "Agendado", 4, "Execução"),
    REPAROS_LIBERADOS("4.2", "Reparos Liberados", 4, "Execução"),
    COMPLEMENTOS("4.3", "Complementos", 4, "Execução"),
    ENTREGUES("4.7", "Entregues", 4, "Execução"),
    PESQUISA_SATISFACAO("4.8", "Pesquisa de Satisfação", 4, "Execução"),

    // Fase 5: Garantia
    ABERTURA_GARANTIA("5.0", "Abertura de Garantia", 5, "Garantia"),
    VISTORIA_GARANTIA("5.1", "Vistoria de Garantia", 5, "Garantia"),
    GARANTIA_AUTORIZADA("5.2", "Garantia Autorizada", 5, "Garantia"),
    GARANTIA_ENTREGUE("5.7", "Garantia Entregue", 5, "Garantia");

    private final String code;
    private final String displayName;
    private final int phase;
    private final String phaseName;

    Status(String code, String displayName, int phase, String phaseName) {
        this.code = code;
        this.displayName = displayName;
        this.phase = phase;
        this.phaseName = phaseName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPhase() {
        return phase;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public String getFullName() {
        return code + " " + displayName;
    }
}
