package com.necsus.necsusspring.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.necsus.necsusspring.dto.ReportConfig;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.repository.PartnerRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    private static final BaseColor UB_PURPLE = new BaseColor(124, 58, 237);
    private static final BaseColor UB_PURPLE_LIGHT = new BaseColor(144, 97, 249);
    private static final BaseColor UB_PURPLE_DARK = new BaseColor(91, 33, 182);
    private static final BaseColor LIGHT_GRAY = new BaseColor(243, 244, 246);

    // Mapeamento de campos para labels amigáveis - Veículos
    private static final Map<String, String> VEHICLE_FIELD_LABELS = new LinkedHashMap<>();
    static {
        VEHICLE_FIELD_LABELS.put("id", "ID");
        VEHICLE_FIELD_LABELS.put("plaque", "Placa");
        VEHICLE_FIELD_LABELS.put("maker", "Montadora");
        VEHICLE_FIELD_LABELS.put("model", "Modelo");
        VEHICLE_FIELD_LABELS.put("type_vehicle", "Tipo de Veículo");
        VEHICLE_FIELD_LABELS.put("color", "Cor");
        VEHICLE_FIELD_LABELS.put("year_mod", "Ano/Modelo");
        VEHICLE_FIELD_LABELS.put("year_maker", "Ano de Fabricação");
        VEHICLE_FIELD_LABELS.put("chassis", "Chassis");
        VEHICLE_FIELD_LABELS.put("ports", "Portas");
        VEHICLE_FIELD_LABELS.put("category", "Categoria");
        VEHICLE_FIELD_LABELS.put("km_vehicle", "Quilometragem");
        VEHICLE_FIELD_LABELS.put("tipo_combustivel", "Tipo de Combustível");
        VEHICLE_FIELD_LABELS.put("transmission", "Transmissão");
        VEHICLE_FIELD_LABELS.put("renavam", "Renavam");
        VEHICLE_FIELD_LABELS.put("fipe_value", "Valor FIPE");
        VEHICLE_FIELD_LABELS.put("date_dut", "Data DUT");
        VEHICLE_FIELD_LABELS.put("dut_named_to", "DUT em Nome de");
        VEHICLE_FIELD_LABELS.put("expedition", "Expedição");
        VEHICLE_FIELD_LABELS.put("breakdowns", "Defeitos");
        VEHICLE_FIELD_LABELS.put("notes", "Observações");
        VEHICLE_FIELD_LABELS.put("contract_begin", "Início do Contrato");
        VEHICLE_FIELD_LABELS.put("contract_end", "Fim do Contrato");
        VEHICLE_FIELD_LABELS.put("codigo_externo", "Código Externo");
        VEHICLE_FIELD_LABELS.put("codigo_fipe", "Código FIPE");
        VEHICLE_FIELD_LABELS.put("vehicleStatus", "Status");
        VEHICLE_FIELD_LABELS.put("partner", "Associado");
    }

    // Mapeamento de campos para labels amigáveis - Associados
    private static final Map<String, String> PARTNER_FIELD_LABELS = new LinkedHashMap<>();
    static {
        PARTNER_FIELD_LABELS.put("id", "ID");
        PARTNER_FIELD_LABELS.put("name", "Nome");
        PARTNER_FIELD_LABELS.put("cpf", "CPF");
        PARTNER_FIELD_LABELS.put("rg", "RG");
        PARTNER_FIELD_LABELS.put("email", "E-mail");
        PARTNER_FIELD_LABELS.put("phone", "Telefone");
        PARTNER_FIELD_LABELS.put("cell", "Celular");
        PARTNER_FIELD_LABELS.put("fax", "Fax");
        PARTNER_FIELD_LABELS.put("dateBorn", "Data de Nascimento");
        PARTNER_FIELD_LABELS.put("status", "Status");
        PARTNER_FIELD_LABELS.put("registrationDate", "Data de Registro");
        PARTNER_FIELD_LABELS.put("contractDate", "Data do Contrato");
        PARTNER_FIELD_LABELS.put("address", "Endereço");
        PARTNER_FIELD_LABELS.put("vehicleCount", "Quantidade de Veículos");
    }

    public byte[] generateVehicleReport(ReportConfig config) {
        try {
            logger.info("Iniciando geração de relatório de veículos");
            List<Vehicle> vehicles = vehicleRepository.findAll();
            logger.info("Total de veículos encontrados: {}", vehicles.size());

            if (vehicles.isEmpty()) {
                logger.warn("Nenhum veículo encontrado para gerar relatório");
                throw new IllegalStateException("Não há veículos cadastrados no sistema. Por favor, cadastre veículos antes de gerar o relatório.");
            }

            List<String> selectedFields = resolveSelectedFields(config.getSelectedFields(), VEHICLE_FIELD_LABELS);
            logger.info("Campos selecionados: {}", selectedFields);

            if ("pdf".equalsIgnoreCase(config.getFormat())) {
                logger.info("Gerando relatório em formato PDF");
                return generateVehiclePDF(vehicles, selectedFields);
            } else {
                logger.info("Gerando relatório em formato Excel");
                return generateVehicleExcel(vehicles, selectedFields);
            }
        } catch (IllegalStateException e) {
            logger.error("Validação falhou: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro ao gerar relatório de veículos: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar relatório de veículos", e);
        }
    }

    public byte[] generatePartnerReport(ReportConfig config) {
        try {
            logger.info("Iniciando geração de relatório de associados");
            List<Partner> partners = partnerRepository.findAll();
            logger.info("Total de associados encontrados: {}", partners.size());

            if (partners.isEmpty()) {
                logger.warn("Nenhum associado encontrado para gerar relatório");
                throw new IllegalStateException("Não há associados cadastrados no sistema. Por favor, cadastre associados antes de gerar o relatório.");
            }

            List<String> selectedFields = resolveSelectedFields(config.getSelectedFields(), PARTNER_FIELD_LABELS);
            logger.info("Campos selecionados: {}", selectedFields);

            if ("pdf".equalsIgnoreCase(config.getFormat())) {
                logger.info("Gerando relatório em formato PDF");
                return generatePartnerPDF(partners, selectedFields);
            } else {
                logger.info("Gerando relatório em formato Excel");
                return generatePartnerExcel(partners, selectedFields);
            }
        } catch (IllegalStateException e) {
            logger.error("Validação falhou: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro ao gerar relatório de associados: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar relatório de associados", e);
        }
    }

    private byte[] generateVehiclePDF(List<Vehicle> vehicles, List<String> selectedFields) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        // Adicionar header e footer
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                try {
                    // Header
                    Rectangle page = document.getPageSize();
                    PdfPTable header = new PdfPTable(1);
                    header.setTotalWidth(page.getWidth() - 40);
                    header.setLockedWidth(true);

                    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, UB_PURPLE);
                    PdfPCell headerCell = new PdfPCell(new Phrase("Relatório de Veículos", headerFont));
                    headerCell.setBorder(Rectangle.NO_BORDER);
                    headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    headerCell.setPaddingBottom(10);
                    header.addCell(headerCell);

                    header.writeSelectedRows(0, -1, 20, page.getHeight() - 20, writer.getDirectContent());

                    // Footer
                    Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);
                    Phrase footer = new Phrase("Gerado em: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()) +
                            " | Página " + writer.getPageNumber(), footerFont);
                    ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                            footer, (page.getLeft() + page.getRight()) / 2, page.getBottom() + 20, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        document.open();

        // Criar tabela
        PdfPTable table = new PdfPTable(selectedFields.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        // Estilos
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);

        // Cabeçalho da tabela
        for (String field : selectedFields) {
            PdfPCell cell = new PdfPCell(new Phrase(VEHICLE_FIELD_LABELS.getOrDefault(field, field), headerFont));
            cell.setBackgroundColor(UB_PURPLE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Dados
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        boolean alternate = false;
        for (Vehicle vehicle : vehicles) {
            for (String field : selectedFields) {
                String value = getVehicleFieldValue(vehicle, field, dateFormat);
                PdfPCell cell = new PdfPCell(new Phrase(value, cellFont));
                cell.setPadding(6);
                if (alternate) {
                    cell.setBackgroundColor(LIGHT_GRAY);
                }
                table.addCell(cell);
            }
            alternate = !alternate;
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generatePartnerPDF(List<Partner> partners, List<String> selectedFields) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        // Adicionar header e footer
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                try {
                    // Header
                    Rectangle page = document.getPageSize();
                    PdfPTable header = new PdfPTable(1);
                    header.setTotalWidth(page.getWidth() - 40);
                    header.setLockedWidth(true);

                    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, UB_PURPLE);
                    PdfPCell headerCell = new PdfPCell(new Phrase("Relatório de Associados", headerFont));
                    headerCell.setBorder(Rectangle.NO_BORDER);
                    headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    headerCell.setPaddingBottom(10);
                    header.addCell(headerCell);

                    header.writeSelectedRows(0, -1, 20, page.getHeight() - 20, writer.getDirectContent());

                    // Footer
                    Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);
                    Phrase footer = new Phrase("Gerado em: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()) +
                            " | Página " + writer.getPageNumber(), footerFont);
                    ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                            footer, (page.getLeft() + page.getRight()) / 2, page.getBottom() + 20, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        document.open();

        // Criar tabela
        PdfPTable table = new PdfPTable(selectedFields.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        // Estilos
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);

        // Cabeçalho da tabela
        for (String field : selectedFields) {
            PdfPCell cell = new PdfPCell(new Phrase(PARTNER_FIELD_LABELS.getOrDefault(field, field), headerFont));
            cell.setBackgroundColor(UB_PURPLE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Dados
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        boolean alternate = false;
        for (Partner partner : partners) {
            for (String field : selectedFields) {
                String value = getPartnerFieldValue(partner, field, dateFormatter, dateTimeFormatter);
                PdfPCell cell = new PdfPCell(new Phrase(value, cellFont));
                cell.setPadding(6);
                if (alternate) {
                    cell.setBackgroundColor(LIGHT_GRAY);
                }
                table.addCell(cell);
            }
            alternate = !alternate;
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }

    private byte[] generateVehicleExcel(List<Vehicle> vehicles, List<String> selectedFields) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Veículos");

        // Estilos
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont(); // POI Font
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.LEFT);

        CellStyle alternateStyle = workbook.createCellStyle();
        alternateStyle.setAlignment(HorizontalAlignment.LEFT);
        alternateStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        alternateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Cabeçalho
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < selectedFields.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(VEHICLE_FIELD_LABELS.getOrDefault(selectedFields.get(i), selectedFields.get(i)));
            cell.setCellStyle(headerStyle);
        }

        // Dados
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        int rowNum = 1;
        for (Vehicle vehicle : vehicles) {
            Row row = sheet.createRow(rowNum);
            for (int i = 0; i < selectedFields.size(); i++) {
                Cell cell = row.createCell(i);
                String value = getVehicleFieldValue(vehicle, selectedFields.get(i), dateFormat);
                cell.setCellValue(value);
                cell.setCellStyle(rowNum % 2 == 0 ? alternateStyle : dataStyle);
            }
            rowNum++;
        }

        // Ajustar largura das colunas
        for (int i = 0; i < selectedFields.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return baos.toByteArray();
    }

    private byte[] generatePartnerExcel(List<Partner> partners, List<String> selectedFields) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Associados");

        // Estilos
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont(); // POI Font
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.LEFT);

        CellStyle alternateStyle = workbook.createCellStyle();
        alternateStyle.setAlignment(HorizontalAlignment.LEFT);
        alternateStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        alternateStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Cabeçalho
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < selectedFields.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(PARTNER_FIELD_LABELS.getOrDefault(selectedFields.get(i), selectedFields.get(i)));
            cell.setCellStyle(headerStyle);
        }

        // Dados
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        int rowNum = 1;
        for (Partner partner : partners) {
            Row row = sheet.createRow(rowNum);
            for (int i = 0; i < selectedFields.size(); i++) {
                Cell cell = row.createCell(i);
                String value = getPartnerFieldValue(partner, selectedFields.get(i), dateFormatter, dateTimeFormatter);
                cell.setCellValue(value);
                cell.setCellStyle(rowNum % 2 == 0 ? alternateStyle : dataStyle);
            }
            rowNum++;
        }

        // Ajustar largura das colunas
        for (int i = 0; i < selectedFields.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return baos.toByteArray();
    }

    private String getVehicleFieldValue(Vehicle vehicle, String field, SimpleDateFormat dateFormat) {
        try {
            return switch (field) {
                case "id" -> vehicle.getId() != null ? vehicle.getId().toString() : "";
                case "plaque" -> vehicle.getPlaque() != null ? vehicle.getPlaque() : "";
                case "maker" -> vehicle.getMaker() != null ? vehicle.getMaker() : "";
                case "model" -> vehicle.getModel() != null ? vehicle.getModel() : "";
                case "type_vehicle" -> vehicle.getType_vehicle() != null ? vehicle.getType_vehicle() : "";
                case "color" -> vehicle.getColor() != null ? vehicle.getColor() : "";
                case "year_mod" -> vehicle.getYear_mod() != null ? vehicle.getYear_mod() : "";
                case "year_maker" -> vehicle.getYear_maker() != null ? vehicle.getYear_maker() : "";
                case "chassis" -> vehicle.getChassis() != null ? vehicle.getChassis() : "";
                case "ports" -> vehicle.getPorts() != null ? vehicle.getPorts().toString() : "";
                case "category" -> vehicle.getCategory() != null ? vehicle.getCategory() : "";
                case "km_vehicle" -> vehicle.getKm_vehicle() != null ? vehicle.getKm_vehicle() : "";
                case "tipo_combustivel" -> vehicle.getTipo_combustivel() != null ? vehicle.getTipo_combustivel() : "";
                case "transmission" -> vehicle.getTransmission() != null ? vehicle.getTransmission() : "";
                case "renavam" -> vehicle.getRenavam() != null ? vehicle.getRenavam() : "";
                case "fipe_value" -> vehicle.getFipe_value() != null ? String.format("R$ %.2f", vehicle.getFipe_value()) : "";
                case "date_dut" -> vehicle.getDate_dut() != null ? dateFormat.format(vehicle.getDate_dut()) : "";
                case "dut_named_to" -> vehicle.getDut_named_to() != null ? vehicle.getDut_named_to() : "";
                case "expedition" -> vehicle.getExpedition() != null ? vehicle.getExpedition() : "";
                case "breakdowns" -> vehicle.getBreakdowns() != null ? vehicle.getBreakdowns() : "";
                case "notes" -> vehicle.getNotes() != null ? vehicle.getNotes() : "";
                case "contract_begin" -> vehicle.getContract_begin() != null ? dateFormat.format(vehicle.getContract_begin()) : "";
                case "contract_end" -> vehicle.getContract_end() != null ? dateFormat.format(vehicle.getContract_end()) : "";
                case "codigo_externo" -> vehicle.getCodigo_externo() != null ? vehicle.getCodigo_externo() : "";
                case "codigo_fipe" -> vehicle.getCodigo_fipe() != null ? vehicle.getCodigo_fipe() : "";
                case "vehicleStatus" -> vehicle.getVehicleStatus() != null ? vehicle.getVehicleStatus().toString() : "";
                case "partner" -> vehicle.getPartner() != null ? vehicle.getPartner().getName() : "";
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private String getPartnerFieldValue(Partner partner, String field, DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter) {
        try {
            return switch (field) {
                case "id" -> partner.getId() != null ? partner.getId().toString() : "";
                case "name" -> partner.getName() != null ? partner.getName() : "";
                case "cpf" -> partner.getCpf() != null ? partner.getCpf() : "";
                case "rg" -> partner.getRg() != null ? partner.getRg() : "";
                case "email" -> partner.getEmail() != null ? partner.getEmail() : "";
                case "phone" -> partner.getPhone() != null ? partner.getPhone() : "";
                case "cell" -> partner.getCell() != null ? partner.getCell() : "";
                case "fax" -> partner.getFax() != null ? partner.getFax() : "";
                case "dateBorn" -> partner.getDateBorn() != null ? partner.getDateBorn().format(dateFormatter) : "";
                case "status" -> partner.getStatus() != null ? partner.getStatus().toString() : "";
                case "registrationDate" -> partner.getRegistrationDate() != null ? partner.getRegistrationDate().format(dateTimeFormatter) : "";
                case "contractDate" -> partner.getContractDate() != null ? partner.getContractDate().format(dateFormatter) : "";
                case "address" -> partner.getAddress() != null ? formatAddress(partner.getAddress()) : "";
                case "vehicleCount" -> partner.getVehicles() != null ? String.valueOf(partner.getVehicles().size()) : "0";
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private String formatAddress(com.necsus.necsusspring.model.Address address) {
        StringBuilder sb = new StringBuilder();
        appendIfNotBlank(sb, address.getEndereco(), "", "");
        appendIfNotBlank(sb, address.getNumero(), sb.length() > 0 ? ", " : "", "");
        appendIfNotBlank(sb, address.getBairro(), sb.length() > 0 ? " - " : "", "");
        appendIfNotBlank(sb, address.getCidade(), sb.length() > 0 ? ", " : "", "");
        appendIfNotBlank(sb, address.getEstado(), sb.length() > 0 ? " - " : "", "");
        appendIfNotBlank(sb, address.getCep(), sb.length() > 0 ? " CEP: " : "CEP: ", "");
        return sb.toString();
    }

    private List<String> resolveSelectedFields(List<String> selectedFields, Map<String, String> fieldLabels) {
        List<String> resolved = new ArrayList<>();

        if (selectedFields != null) {
            for (String field : selectedFields) {
                if (fieldLabels.containsKey(field)) {
                    resolved.add(field);
                }
            }
        }

        if (resolved.isEmpty()) {
            resolved.addAll(fieldLabels.keySet());
        }

        return resolved;
    }

    private void appendIfNotBlank(StringBuilder sb, String value, String prefix, String suffix) {
        if (value != null && !value.isBlank()) {
            sb.append(prefix).append(value).append(suffix);
        }
    }

    public Map<String, String> getVehicleFieldLabels() {
        return VEHICLE_FIELD_LABELS;
    }

    public Map<String, String> getPartnerFieldLabels() {
        return PARTNER_FIELD_LABELS;
    }
}
