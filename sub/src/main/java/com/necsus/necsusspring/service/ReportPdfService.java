package com.necsus.necsusspring.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.model.VehicleStatus;
import com.necsus.necsusspring.service.ReportDataService.PartnerReportData;
import com.necsus.necsusspring.service.ReportDataService.VehicleReportData;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportPdfService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
    private static final Font SECTION_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font TEXT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font STRONG_TEXT_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

    public byte[] generatePartnerReport(PartnerReportData data, List<String> selectedFields) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            addReportHeader(document, "Relatório de Associados");
            addPartnerSummary(document, data);
            addPartnerTable(document, data, normalizeSelection(selectedFields));
            addPartnerInsights(document, data);

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Não foi possível gerar o PDF de associados.", exception);
        }
    }

    public byte[] generateVehicleReport(VehicleReportData data, List<String> selectedFields) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 54);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            addReportHeader(document, "Relatório de Veículos");
            addVehicleSummary(document, data);
            addVehicleTable(document, data, normalizeSelection(selectedFields));
            addVehicleInsights(document, data);

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Não foi possível gerar o PDF de veículos.", exception);
        }
    }

    private Set<String> normalizeSelection(List<String> selectedFields) {
        if (selectedFields == null || selectedFields.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(selectedFields);
    }

    private void addReportHeader(Document document, String title) throws DocumentException {
        Paragraph header = new Paragraph(title, TITLE_FONT);
        header.setAlignment(Element.ALIGN_LEFT);
        header.setSpacingAfter(4f);
        document.add(header);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        Paragraph subtitle = new Paragraph("Gerado em " + LocalDateTime.now().format(formatter), SUBTITLE_FONT);
        subtitle.setSpacingAfter(16f);
        document.add(subtitle);
    }

    private void addPartnerSummary(Document document, PartnerReportData data) throws DocumentException {
        Paragraph summaryTitle = new Paragraph("Resumo geral", SECTION_TITLE_FONT);
        summaryTitle.setSpacingAfter(8f);
        document.add(summaryTitle);

        PdfPTable summaryTable = new PdfPTable(new float[]{3f, 3f, 3f, 3f});
        summaryTable.setWidthPercentage(100f);
        summaryTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        summaryTable.addCell(buildSummaryMetric("Associados", String.valueOf(data.totalPartners())));
        summaryTable.addCell(buildSummaryMetric("Com veículos", String.valueOf(data.partnersWithVehicles())));
        summaryTable.addCell(buildSummaryMetric("Sem veículos", String.valueOf(data.partnersWithoutVehicles())));
        summaryTable.addCell(buildSummaryMetric("Média de veículos", data.averageVehiclesPerPartner()));

        document.add(summaryTable);
        document.add(Chunk.NEWLINE);
    }

    private void addVehicleSummary(Document document, VehicleReportData data) throws DocumentException {
        Paragraph summaryTitle = new Paragraph("Resumo geral", SECTION_TITLE_FONT);
        summaryTitle.setSpacingAfter(8f);
        document.add(summaryTitle);

        PdfPTable summaryTable = new PdfPTable(new float[]{3f, 3f, 3f, 3f});
        summaryTable.setWidthPercentage(100f);
        summaryTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        summaryTable.addCell(buildSummaryMetric("Veículos cadastrados", String.valueOf(data.totalVehicles())));
        summaryTable.addCell(buildSummaryMetric("Associados distintos", String.valueOf(data.distinctPartners())));
        summaryTable.addCell(buildSummaryMetric("Total mensalidades", data.totalMonthlyValueFormatted()));
        summaryTable.addCell(buildSummaryMetric("Média mensal", calculateAverageMonthly(data)));

        document.add(summaryTable);
        document.add(Chunk.NEWLINE);
    }

    private String calculateAverageMonthly(VehicleReportData data) {
        if (data.vehicles().isEmpty()) {
            return "R$ 0,00";
        }

        BigDecimal average = data.totalMonthlyValue()
                .divide(BigDecimal.valueOf(data.vehicles().size()), 2, RoundingMode.HALF_UP);
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return currency.format(average);
    }

    private PdfPCell buildSummaryMetric(String label, String value) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Phrase(label + "\n", SUBTITLE_FONT));
        paragraph.add(new Phrase(value, STRONG_TEXT_FONT));

        PdfPCell cell = new PdfPCell(paragraph);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8f);
        return cell;
    }

    private void addPartnerTable(Document document, PartnerReportData data, Set<String> selectedFields) throws DocumentException {
        if (!data.hasPartners()) {
            Paragraph message = new Paragraph("Não há associados cadastrados para gerar o relatório.", TEXT_FONT);
            document.add(message);
            return;
        }

        Paragraph sectionTitle = new Paragraph("Detalhamento dos associados", SECTION_TITLE_FONT);
        sectionTitle.setSpacingAfter(6f);
        document.add(sectionTitle);

        List<ReportColumn<Partner>> baseColumns = List.of(
                new ReportColumn<>("name", "Nome", partner -> safeText(partner.getName())),
                new ReportColumn<>("cpf", "CPF", partner -> safeText(partner.getCpf())),
                new ReportColumn<>("email", "E-mail", partner -> safeText(partner.getEmail())),
                new ReportColumn<>("contact", "Contato", this::resolvePartnerContact),
                new ReportColumn<>("city", "Cidade", partner -> partner.getAddress() != null ? safeText(partner.getAddress().getCity()) : "Não informado"),
                new ReportColumn<>("vehicleCount", "Veículos", partner -> String.valueOf(data.partnerVehicleCount().getOrDefault(partner.getId(), 0))),
                new ReportColumn<>("status", "Status", partner -> partner.getStatus() != null ? partner.getStatus().getDisplayName() : "Não informado"),
                new ReportColumn<>("registration", "Cadastro", partner -> formatDateTime(partner.getRegistrationDate()))
        );

        List<ReportColumn<Partner>> optionalColumns = List.of(
                new ReportColumn<>("dateBorn", "Data de nascimento", partner -> formatDate(partner.getDateBorn())),
                new ReportColumn<>("rg", "RG", partner -> safeText(partner.getRg(), "Não informado")),
                new ReportColumn<>("phone", "Telefone fixo", partner -> safeText(partner.getPhone(), "Não informado")),
                new ReportColumn<>("contractDate", "Data do contrato", partner -> formatDate(partner.getContractDate())),
                new ReportColumn<>("company", "Empresa vinculada", partner -> partner.getCompany() != null ? safeText(partner.getCompany().getCompanyName(), "Não informado") : "Não informado"),
                new ReportColumn<>("fullAddress", "Endereço completo", partner -> data.partnerAddressSummary().getOrDefault(partner.getId(), "Não informado")),
                new ReportColumn<>("documents", "Documentos enviados", partner -> partner.getDocumentPaths() != null ? partner.getDocumentPaths().size() + " arquivo(s)" : "0 arquivo(s)"),
                new ReportColumn<>("fax", "Fax", partner -> safeText(partner.getFax(), "Não informado"))
        );

        List<ReportColumn<Partner>> columns = buildColumns(baseColumns, optionalColumns, selectedFields);
        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100f);

        for (ReportColumn<Partner> column : columns) {
            table.addCell(buildHeaderCell(column.header()));
        }

        for (Partner partner : data.partners()) {
            for (ReportColumn<Partner> column : columns) {
                table.addCell(buildBodyCell(column.extractor().apply(partner)));
            }
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addVehicleTable(Document document, VehicleReportData data, Set<String> selectedFields) throws DocumentException {
        if (!data.hasVehicles()) {
            Paragraph message = new Paragraph("Não há veículos cadastrados para gerar o relatório.", TEXT_FONT);
            document.add(message);
            return;
        }

        Paragraph sectionTitle = new Paragraph("Detalhamento dos veículos", SECTION_TITLE_FONT);
        sectionTitle.setSpacingAfter(6f);
        document.add(sectionTitle);

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        List<ReportColumn<Vehicle>> baseColumns = List.of(
                new ReportColumn<>("partner", "Associado", vehicle -> vehicle.getPartner() != null ? safeText(vehicle.getPartner().getName()) : "Não informado"),
                new ReportColumn<>("plaque", "Placa", vehicle -> safeText(vehicle.getPlaque())),
                new ReportColumn<>("model", "Modelo", vehicle -> safeText(vehicle.getModel())),
                new ReportColumn<>("maker", "Montadora", vehicle -> safeText(vehicle.getMaker())),
                new ReportColumn<>("color", "Cor", vehicle -> safeText(vehicle.getColor())),
                new ReportColumn<>("year", "Ano/Modelo", vehicle -> safeText(vehicle.getYear_mod())),
                new ReportColumn<>("fuel", "Combustível", vehicle -> safeText(vehicle.getTipo_combustivel())),
                new ReportColumn<>("monthly", "Mensalidade", vehicle -> formatMonthly(vehicle, currency))
        );

        List<ReportColumn<Vehicle>> optionalColumns = List.of(
                new ReportColumn<>("vehicleStatus", "Status do veículo", vehicle -> resolveVehicleStatus(vehicle)),
                new ReportColumn<>("chassis", "Chassi", vehicle -> safeText(vehicle.getChassis(), "Não informado")),
                new ReportColumn<>("category", "Categoria", vehicle -> safeText(vehicle.getCategory(), "Não informado")),
                new ReportColumn<>("contractBegin", "Contrato - início", vehicle -> formatDate(vehicle.getContract_begin(), dateFormat)),
                new ReportColumn<>("contractEnd", "Contrato - término", vehicle -> formatDate(vehicle.getContract_end(), dateFormat)),
                new ReportColumn<>("fipeValue", "Valor FIPE", vehicle -> formatCurrency(vehicle.getFipe_value(), currency)),
                new ReportColumn<>("renavam", "Renavam", vehicle -> safeText(vehicle.getRenavam(), "Não informado")),
                new ReportColumn<>("notes", "Observações", vehicle -> safeText(vehicle.getNotes(), "Sem observações"))
        );

        List<ReportColumn<Vehicle>> columns = buildColumns(baseColumns, optionalColumns, selectedFields);

        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100f);

        for (ReportColumn<Vehicle> column : columns) {
            table.addCell(buildHeaderCell(column.header()));
        }

        for (Vehicle vehicle : data.vehicles()) {
            for (ReportColumn<Vehicle> column : columns) {
                table.addCell(buildBodyCell(column.extractor().apply(vehicle)));
            }
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private <T> List<ReportColumn<T>> buildColumns(List<ReportColumn<T>> baseColumns,
                                                   List<ReportColumn<T>> optionalColumns,
                                                   Set<String> selectedFields) {
        List<ReportColumn<T>> result = baseColumns.stream().collect(Collectors.toList());
        if (selectedFields.isEmpty()) {
            return result;
        }

        for (ReportColumn<T> column : optionalColumns) {
            if (selectedFields.contains(column.key())) {
                result.add(column);
            }
        }
        return result;
    }

    private void addPartnerInsights(Document document, PartnerReportData data) throws DocumentException {
        if (data.partnersByCity().isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("Cidades com mais associados", SECTION_TITLE_FONT);
        sectionTitle.setSpacingAfter(6f);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(new float[]{6f, 2f});
        table.setWidthPercentage(60f);

        table.addCell(buildHeaderCell("Cidade"));
        table.addCell(buildHeaderCell("Associados"));

        int limit = 0;
        for (Map.Entry<String, Long> entry : data.partnersByCity().entrySet()) {
            if (limit++ == 5) {
                break;
            }
            table.addCell(buildBodyCell(entry.getKey()));
            table.addCell(buildBodyCell(String.valueOf(entry.getValue())));
        }

        document.add(table);
    }

    private void addVehicleInsights(Document document, VehicleReportData data) throws DocumentException {
        boolean hasInsights = !data.vehiclesByFuel().isEmpty() || !data.vehiclesByMaker().isEmpty();
        if (!hasInsights) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("Distribuição da frota", SECTION_TITLE_FONT);
        sectionTitle.setSpacingAfter(6f);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(new float[]{4f, 2f, 4f, 2f});
        table.setWidthPercentage(100f);

        table.addCell(buildHeaderCell("Combustível"));
        table.addCell(buildHeaderCell("Veículos"));
        table.addCell(buildHeaderCell("Montadora"));
        table.addCell(buildHeaderCell("Veículos"));

        int maxRows = Math.max(data.vehiclesByFuel().size(), data.vehiclesByMaker().size());
        List<Map.Entry<String, Long>> fuelEntries = data.vehiclesByFuel().entrySet().stream().collect(Collectors.toList());
        List<Map.Entry<String, Long>> makerEntries = data.vehiclesByMaker().entrySet().stream().collect(Collectors.toList());

        for (int i = 0; i < maxRows; i++) {
            if (i < fuelEntries.size()) {
                table.addCell(buildBodyCell(fuelEntries.get(i).getKey()));
                table.addCell(buildBodyCell(String.valueOf(fuelEntries.get(i).getValue())));
            } else {
                table.addCell(buildEmptyCell());
                table.addCell(buildEmptyCell());
            }

            if (i < makerEntries.size()) {
                table.addCell(buildBodyCell(makerEntries.get(i).getKey()));
                table.addCell(buildBodyCell(String.valueOf(makerEntries.get(i).getValue())));
            } else {
                table.addCell(buildEmptyCell());
                table.addCell(buildEmptyCell());
            }
        }

        document.add(table);
    }

    private PdfPCell buildHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new Color(33, 150, 243));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        return cell;
    }

    private PdfPCell buildBodyCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TEXT_FONT));
        cell.setPadding(6f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell buildEmptyCell() {
        PdfPCell cell = new PdfPCell(new Phrase("", TEXT_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private String resolvePartnerContact(Partner partner) {
        if (partner.getCell() != null && !partner.getCell().isBlank()) {
            return partner.getCell();
        }
        if (partner.getPhone() != null && !partner.getPhone().isBlank()) {
            return partner.getPhone();
        }
        return "Não informado";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }

    private String formatDate(java.util.Date date, SimpleDateFormat formatter) {
        if (date == null) {
            return "-";
        }
        return formatter.format(date);
    }

    private String formatMonthly(Vehicle vehicle, NumberFormat currency) {
        if (vehicle.getPayment() == null || vehicle.getPayment().getMonthly() == null) {
            return "-";
        }
        return currency.format(vehicle.getPayment().getMonthly());
    }

    private String formatCurrency(Double value, NumberFormat currency) {
        if (value == null) {
            return "-";
        }
        return currency.format(value);
    }

    private String resolveVehicleStatus(Vehicle vehicle) {
        if (vehicle.getVehicleStatus() != null) {
            return vehicle.getVehicleStatus().getDisplayName();
        }
        if (vehicle.getStatus() != null) {
            try {
                return VehicleStatus.fromId(vehicle.getStatus()).getDisplayName();
            } catch (IllegalArgumentException ignored) {
                // Ignored - fallback below
            }
        }
        return "Não informado";
    }

    private String safeText(String value) {
        return safeText(value, "-");
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private record ReportColumn<T>(String key, String header, Function<T, String> extractor) {
    }
}

