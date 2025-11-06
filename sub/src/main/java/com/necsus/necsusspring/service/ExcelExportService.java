package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Partner;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Gera um arquivo Excel com o relatório completo de associados
     */
    public byte[] generatePartnerReportExcel(List<Partner> partners) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Cria a planilha
            Sheet sheet = workbook.createSheet("Relatório de Associados");

            // Estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Cria cabeçalho
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "ID", "Nome", "CPF", "RG", "Email", "Telefone", "Celular",
                "Data Nascimento", "Status", "Data Cadastro", "Data Contrato",
                "Endereço", "Número", "Bairro", "Cidade", "Estado", "CEP",
                "Quantidade de Veículos", "Empresa"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Preenche dados
            int rowNum = 1;
            for (Partner partner : partners) {
                Row row = sheet.createRow(rowNum++);

                // ID
                createCell(row, 0, partner.getId() != null ? partner.getId().toString() : "", dataStyle);

                // Nome
                createCell(row, 1, partner.getName(), dataStyle);

                // CPF
                createCell(row, 2, partner.getCpf(), dataStyle);

                // RG
                createCell(row, 3, partner.getRg(), dataStyle);

                // Email
                createCell(row, 4, partner.getEmail(), dataStyle);

                // Telefone
                createCell(row, 5, partner.getPhone(), dataStyle);

                // Celular
                createCell(row, 6, partner.getCell(), dataStyle);

                // Data Nascimento
                if (partner.getDateBorn() != null) {
                    createCell(row, 7, partner.getDateBorn().format(DATE_FORMATTER), dateStyle);
                } else {
                    createCell(row, 7, "", dateStyle);
                }

                // Status
                createCell(row, 8, partner.getStatus() != null ? partner.getStatus().getDisplayName() : "", dataStyle);

                // Data Cadastro
                if (partner.getRegistrationDate() != null) {
                    createCell(row, 9, partner.getRegistrationDate().format(DATE_TIME_FORMATTER), dateStyle);
                } else {
                    createCell(row, 9, "", dateStyle);
                }

                // Data Contrato
                if (partner.getContractDate() != null) {
                    createCell(row, 10, partner.getContractDate().format(DATE_FORMATTER), dateStyle);
                } else {
                    createCell(row, 10, "", dateStyle);
                }

                // Endereço completo
                if (partner.getAddress() != null) {
                    createCell(row, 11, partner.getAddress().getAddress(), dataStyle);
                    createCell(row, 12, partner.getAddress().getNumber(), dataStyle);
                    createCell(row, 13, partner.getAddress().getNeighborhood(), dataStyle);
                    createCell(row, 14, partner.getAddress().getCity(), dataStyle);
                    createCell(row, 15, partner.getAddress().getStates(), dataStyle);
                    createCell(row, 16, partner.getAddress().getZipcode(), dataStyle);
                } else {
                    for (int i = 11; i <= 16; i++) {
                        createCell(row, i, "", dataStyle);
                    }
                }

                // Quantidade de veículos
                int vehicleCount = partner.getVehicles() != null ? partner.getVehicles().size() : 0;
                createCell(row, 17, String.valueOf(vehicleCount), dataStyle);

                // Empresa
                createCell(row, 18,
                    partner.getCompany() != null ? partner.getCompany().getCompanyName() : "",
                    dataStyle);
            }

            // Ajusta largura das colunas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                // Adiciona um pouco de espaço extra
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Gera um arquivo Excel com resumo estatístico dos associados
     */
    public byte[] generatePartnerSummaryExcel(ReportDataService.PartnerReportData reportData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Planilha 1: Resumo Geral
            Sheet summarySheet = workbook.createSheet("Resumo Geral");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;

            // Título
            Row titleRow = summarySheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RELATÓRIO DE ASSOCIADOS - RESUMO GERAL");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            rowNum++; // linha em branco

            // Indicadores principais
            createSummaryRow(summarySheet, rowNum++, "Total de Associados",
                String.valueOf(reportData.totalPartners()), headerStyle, dataStyle);
            createSummaryRow(summarySheet, rowNum++, "Associados com Veículos",
                String.valueOf(reportData.partnersWithVehicles()), headerStyle, dataStyle);
            createSummaryRow(summarySheet, rowNum++, "Associados sem Veículos",
                String.valueOf(reportData.partnersWithoutVehicles()), headerStyle, dataStyle);
            createSummaryRow(summarySheet, rowNum++, "Total de Veículos",
                String.valueOf(reportData.totalVehicles()), headerStyle, dataStyle);
            createSummaryRow(summarySheet, rowNum++, "Média de Veículos por Associado",
                reportData.averageVehiclesPerPartner(), headerStyle, dataStyle);

            rowNum++; // linha em branco

            // Planilha 2: Distribuição por Cidade
            if (reportData.partnersByCity() != null && !reportData.partnersByCity().isEmpty()) {
                Sheet citySheet = workbook.createSheet("Distribuição por Cidade");
                createDistributionSheet(citySheet, reportData.partnersByCity(),
                    "DISTRIBUIÇÃO DE ASSOCIADOS POR CIDADE", workbook);
            }

            // Planilha 3: Lista completa de associados
            generatePartnerListSheet(workbook, reportData.partners());

            // Ajusta largura das colunas
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);
            summarySheet.setColumnWidth(0, summarySheet.getColumnWidth(0) + 2000);
            summarySheet.setColumnWidth(1, summarySheet.getColumnWidth(1) + 1000);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void generatePartnerListSheet(Workbook workbook, List<Partner> partners) {
        Sheet sheet = workbook.createSheet("Lista de Associados");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Cabeçalho
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Nome", "CPF", "Email", "Telefone", "Cidade", "Status", "Veículos"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Dados
        int rowNum = 1;
        for (Partner partner : partners) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, partner.getId() != null ? partner.getId().toString() : "", dataStyle);
            createCell(row, 1, partner.getName(), dataStyle);
            createCell(row, 2, partner.getCpf(), dataStyle);
            createCell(row, 3, partner.getEmail(), dataStyle);
            createCell(row, 4, partner.getCell() != null ? partner.getCell() : partner.getPhone(), dataStyle);
            createCell(row, 5, partner.getAddress() != null ? partner.getAddress().getCity() : "", dataStyle);
            createCell(row, 6, partner.getStatus() != null ? partner.getStatus().getDisplayName() : "", dataStyle);

            int vehicleCount = partner.getVehicles() != null ? partner.getVehicles().size() : 0;
            createCell(row, 7, String.valueOf(vehicleCount), dataStyle);
        }

        // Ajusta largura
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }

    private void createDistributionSheet(Sheet sheet, java.util.Map<String, Long> distribution,
                                        String title, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        int rowNum = 0;

        // Título
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
        rowNum++; // linha em branco

        // Cabeçalho
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("Cidade");
        headerCell1.setCellStyle(headerStyle);

        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("Quantidade");
        headerCell2.setCellStyle(headerStyle);

        // Dados
        for (java.util.Map.Entry<String, Long> entry : distribution.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, entry.getKey(), dataStyle);
            createCell(row, 1, entry.getValue().toString(), dataStyle);
        }

        // Ajusta largura
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(0, sheet.getColumnWidth(0) + 2000);
        sheet.setColumnWidth(1, sheet.getColumnWidth(1) + 1000);
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, String value,
                                 CellStyle headerStyle, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(dataStyle);
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}
