package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Partner;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço para exportação de relatórios em formato CSV (compatível com Excel)
 * Nota: Gera arquivos CSV com codificação UTF-8 e BOM para correta abertura no Excel
 */
@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String CSV_SEPARATOR = ";";
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /**
     * Gera um arquivo CSV com o relatório completo de associados (compatível com Excel)
     */
    public byte[] generatePartnerReportExcel(List<Partner> partners) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Adiciona BOM para que o Excel reconheça UTF-8
        baos.write(UTF8_BOM);

        PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8);

        // Cabeçalho
        writer.println(String.join(CSV_SEPARATOR,
            "ID", "Nome", "CPF", "RG", "Email", "Telefone", "Celular",
            "Data Nascimento", "Status", "Data Cadastro", "Data Contrato",
            "Endereço", "Número", "Bairro", "Cidade", "Estado", "CEP",
            "Quantidade de Veículos", "Empresa"
        ));

        // Dados
        for (Partner partner : partners) {
            writer.println(String.join(CSV_SEPARATOR,
                csvValue(partner.getId()),
                csvValue(partner.getName()),
                csvValue(partner.getCpf()),
                csvValue(partner.getRg()),
                csvValue(partner.getEmail()),
                csvValue(partner.getPhone()),
                csvValue(partner.getCell()),
                csvValue(partner.getDateBorn() != null ? partner.getDateBorn().format(DATE_FORMATTER) : ""),
                csvValue(partner.getStatus() != null ? partner.getStatus().getDisplayName() : ""),
                csvValue(partner.getRegistrationDate() != null ? partner.getRegistrationDate().format(DATE_TIME_FORMATTER) : ""),
                csvValue(partner.getContractDate() != null ? partner.getContractDate().format(DATE_FORMATTER) : ""),
                csvValue(partner.getAddress() != null ? partner.getAddress().getAddress() : ""),
                csvValue(partner.getAddress() != null ? partner.getAddress().getNumber() : ""),
                csvValue(partner.getAddress() != null ? partner.getAddress().getNeighborhood() : ""),
                csvValue(partner.getAddress() != null ? partner.getAddress().getCity() : ""),
                csvValue(partner.getAddress() != null ? partner.getAddress().getStates() : ""),
                csvValue(partner.getAddress() != null ? partner.getAddress().getZipcode() : ""),
                csvValue(String.valueOf(partner.getVehicles() != null ? partner.getVehicles().size() : 0)),
                csvValue(partner.getCompany() != null ? partner.getCompany().getCompanyName() : "")
            ));
        }

        writer.flush();
        return baos.toByteArray();
    }

    /**
     * Gera um arquivo CSV com resumo estatístico dos associados (compatível com Excel)
     */
    public byte[] generatePartnerSummaryExcel(ReportDataService.PartnerReportData reportData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Adiciona BOM para que o Excel reconheça UTF-8
        baos.write(UTF8_BOM);

        PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8);

        // Seção 1: Resumo Geral
        writer.println("RELATÓRIO DE ASSOCIADOS - RESUMO GERAL");
        writer.println();
        writer.println("Indicador" + CSV_SEPARATOR + "Valor");
        writer.println("Total de Associados" + CSV_SEPARATOR + reportData.totalPartners());
        writer.println("Associados com Veículos" + CSV_SEPARATOR + reportData.partnersWithVehicles());
        writer.println("Associados sem Veículos" + CSV_SEPARATOR + reportData.partnersWithoutVehicles());
        writer.println("Total de Veículos" + CSV_SEPARATOR + reportData.totalVehicles());
        writer.println("Média de Veículos por Associado" + CSV_SEPARATOR + reportData.averageVehiclesPerPartner());
        writer.println();
        writer.println();

        // Seção 2: Distribuição por Cidade
        if (reportData.partnersByCity() != null && !reportData.partnersByCity().isEmpty()) {
            writer.println("DISTRIBUIÇÃO DE ASSOCIADOS POR CIDADE");
            writer.println();
            writer.println("Cidade" + CSV_SEPARATOR + "Quantidade");
            for (var entry : reportData.partnersByCity().entrySet()) {
                writer.println(csvValue(entry.getKey()) + CSV_SEPARATOR + entry.getValue());
            }
            writer.println();
            writer.println();
        }

        // Seção 3: Lista de Associados
        writer.println("LISTA DE ASSOCIADOS");
        writer.println();
        writer.println(String.join(CSV_SEPARATOR,
            "ID", "Nome", "CPF", "Email", "Telefone", "Cidade", "Status", "Veículos"
        ));

        for (Partner partner : reportData.partners()) {
            writer.println(String.join(CSV_SEPARATOR,
                csvValue(partner.getId()),
                csvValue(partner.getName()),
                csvValue(partner.getCpf()),
                csvValue(partner.getEmail()),
                csvValue(partner.getCell() != null ? partner.getCell() : partner.getPhone()),
                csvValue(partner.getAddress() != null ? partner.getAddress().getCity() : ""),
                csvValue(partner.getStatus() != null ? partner.getStatus().getDisplayName() : ""),
                csvValue(String.valueOf(partner.getVehicles() != null ? partner.getVehicles().size() : 0))
            ));
        }

        writer.flush();
        return baos.toByteArray();
    }

    /**
     * Escapa valores para CSV, tratando vírgulas, aspas e quebras de linha
     */
    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        String str = value.toString();

        // Se contém separador, aspas ou quebra de linha, envolve em aspas e escapa aspas internas
        if (str.contains(CSV_SEPARATOR) || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }

        return str;
    }
}
