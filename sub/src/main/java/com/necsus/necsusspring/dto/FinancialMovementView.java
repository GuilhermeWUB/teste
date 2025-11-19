package com.necsus.necsusspring.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Representa um lançamento financeiro já realizado (entrada ou saída)
 * para ser exibido em timelines/resumos das telas do módulo Financeiro.
 */
public class FinancialMovementView {

    private final String tipo;
    private final String titulo;
    private final String subtitulo;
    private final Date data;
    private final BigDecimal valor;
    private final boolean entrada;

    public FinancialMovementView(String tipo, String titulo, String subtitulo, Date data, BigDecimal valor, boolean entrada) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.data = data;
        this.valor = valor;
        this.entrada = entrada;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSubtitulo() {
        return subtitulo;
    }

    public Date getData() {
        return data;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public boolean isEntrada() {
        return entrada;
    }
}
