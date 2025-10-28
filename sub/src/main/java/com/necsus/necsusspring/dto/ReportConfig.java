package com.necsus.necsusspring.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReportConfig {
    private String reportType; // "vehicle" ou "partner"
    private String format; // "pdf" ou "excel"
    private List<String> selectedFields; // Campos selecionados para o relatório
}
