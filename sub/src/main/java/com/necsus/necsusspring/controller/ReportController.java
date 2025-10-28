package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.model.VehicleStatus;
import com.necsus.necsusspring.service.ReportDataService;
import com.necsus.necsusspring.service.ReportDataService.PartnerReportData;
import com.necsus.necsusspring.service.ReportDataService.VehicleReportData;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final Set<String> VEHICLE_SECTIONS = Set.of("summary", "fuelDistribution", "makerDistribution", "vehicleList");
    private static final Set<String> PARTNER_SECTIONS = Set.of("summary", "topCities", "generalIndicators", "partnerList");

    private final ReportDataService reportDataService;

    public ReportController(ReportDataService reportDataService) {
        this.reportDataService = reportDataService;
    }

    @GetMapping
    public String redirectToDefault() {
        return "redirect:/reports/vehicles";
    }

    @GetMapping("/vehicles")
    public String vehicleReport(Model model) {
        model.addAttribute("pageTitle", "SUB - Relatório de Veículos");
        return "relatorio_veiculos";
    }

    @GetMapping("/partners")
    public String partnerReport(Model model) {
        model.addAttribute("pageTitle", "SUB - Relatório de Associados");
        return "relatorio_associados";
    }

    @GetMapping("/vehicles/filters")
    public String vehicleFilters() {
        return "fragments/report_filters :: vehicleFilters";
    }

    @GetMapping("/partners/filters")
    public String partnerFilters() {
        return "fragments/report_filters :: partnerFilters";
    }

    @GetMapping(value = "/vehicles/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> vehicleReportData(
            @RequestParam(value = "sections", required = false) List<String> sections) {

        VehicleReportData reportData = reportDataService.loadVehicleReportData();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hasVehicles", reportData.hasVehicles());

        if (!reportData.hasVehicles()) {
            response.put("message", "Não há veículos cadastrados para gerar o relatório.");
            return ResponseEntity.ok(response);
        }

        Set<String> selectedSections = normalizeSections(sections, VEHICLE_SECTIONS);

        if (selectedSections.contains("summary")) {
            response.put("summary", buildVehicleSummary(reportData));
        }
        if (selectedSections.contains("fuelDistribution")) {
            response.put("fuelDistribution", buildDistributionList(reportData.vehiclesByFuel()));
        }
        if (selectedSections.contains("makerDistribution")) {
            response.put("makerDistribution", buildDistributionList(reportData.vehiclesByMaker()));
        }
        if (selectedSections.contains("vehicleList")) {
            response.put("vehicles", reportData.vehicles().stream()
                    .map(this::buildVehicleRow)
                    .collect(Collectors.toList()));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/partners/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> partnerReportData(
            @RequestParam(value = "sections", required = false) List<String> sections) {

        PartnerReportData reportData = reportDataService.loadPartnerReportData();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hasPartners", reportData.hasPartners());

        if (!reportData.hasPartners()) {
            response.put("message", "Não há associados cadastrados para gerar o relatório.");
            return ResponseEntity.ok(response);
        }

        Set<String> selectedSections = normalizeSections(sections, PARTNER_SECTIONS);

        if (selectedSections.contains("summary")) {
            response.put("summary", buildPartnerSummary(reportData));
        }
        if (selectedSections.contains("topCities")) {
            response.put("topCities", buildDistributionList(reportData.partnersByCity()));
        }
        if (selectedSections.contains("generalIndicators")) {
            response.put("generalIndicators", buildPartnerIndicators(reportData));
        }
        if (selectedSections.contains("partnerList")) {
            response.put("partners", reportData.partners().stream()
                    .map(partner -> buildPartnerRow(partner, reportData))
                    .collect(Collectors.toList()));
        }

        return ResponseEntity.ok(response);
    }

    private Set<String> normalizeSections(List<String> sections, Set<String> allowed) {
        if (sections == null || sections.isEmpty()) {
            return allowed;
        }

        Set<String> normalized = sections.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty() && allowed.contains(value))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return normalized.isEmpty() ? allowed : normalized;
    }

    private Map<String, Object> buildVehicleSummary(VehicleReportData data) {
        Map<String, Object> summary = new LinkedHashMap<>();
        NumberFormat currency = NumberFormat.getCurrencyInstance(PT_BR);

        summary.put("totalVehicles", data.totalVehicles());
        summary.put("distinctPartners", data.distinctPartners());
        summary.put("totalMonthlyValueFormatted", data.totalMonthlyValueFormatted());

        BigDecimal average = data.totalVehicles() > 0
                ? data.totalMonthlyValue().divide(BigDecimal.valueOf(data.totalVehicles()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        summary.put("averageMonthlyValue", currency.format(average));

        return summary;
    }

    private Map<String, Object> buildPartnerSummary(PartnerReportData data) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalPartners", data.totalPartners());
        summary.put("partnersWithVehicles", data.partnersWithVehicles());
        summary.put("partnersWithoutVehicles", data.partnersWithoutVehicles());
        summary.put("averageVehiclesPerPartner", data.averageVehiclesPerPartner());
        return summary;
    }

    private Map<String, Object> buildPartnerIndicators(PartnerReportData data) {
        Map<String, Object> indicators = new LinkedHashMap<>();
        NumberFormat percent = NumberFormat.getNumberInstance(PT_BR);
        percent.setMinimumFractionDigits(1);
        percent.setMaximumFractionDigits(1);

        indicators.put("totalVehicles", data.totalVehicles());
        double activeRate = data.totalPartners() == 0
                ? 0d
                : (data.partnersWithVehicles() * 100.0) / data.totalPartners();
        indicators.put("activePercentage", percent.format(activeRate) + "%");
        return indicators;
    }

    private List<Map<String, Object>> buildDistributionList(Map<String, Long> source) {
        return source.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("label", entry.getKey());
                    map.put("value", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildVehicleRow(Vehicle vehicle) {
        Map<String, Object> row = new LinkedHashMap<>();
        NumberFormat currency = NumberFormat.getCurrencyInstance(PT_BR);

        row.put("id", vehicle.getId());
        row.put("maker", safeValue(vehicle.getMaker()));
        row.put("model", safeValue(vehicle.getModel()));
        row.put("plaque", safeValue(vehicle.getPlaque()));
        row.put("fuelType", safeValue(vehicle.getTipo_combustivel()));
        row.put("status", resolveVehicleStatus(vehicle));
        row.put("partnerName", vehicle.getPartner() != null ? safeValue(vehicle.getPartner().getName()) : null);

        if (vehicle.getPayment() != null && vehicle.getPayment().getMonthly() != null) {
            row.put("monthlyValue", currency.format(vehicle.getPayment().getMonthly()));
        } else {
            row.put("monthlyValue", null);
        }

        return row;
    }

    private Map<String, Object> buildPartnerRow(Partner partner, PartnerReportData data) {
        Map<String, Object> row = new LinkedHashMap<>();

        row.put("id", partner.getId());
        row.put("name", safeValue(partner.getName()));
        row.put("email", safeValue(partner.getEmail()));
        row.put("cpf", safeValue(partner.getCpf()));
        row.put("status", resolvePartnerStatus(partner));
        row.put("vehicleCount", data.partnerVehicleCount().getOrDefault(partner.getId(), 0));

        if (partner.getAddress() != null) {
            row.put("city", safeValue(partner.getAddress().getCity()));
        } else {
            row.put("city", null);
        }

        LocalDateTime registrationDate = partner.getRegistrationDate();
        row.put("registrationDate", registrationDate != null ? registrationDate.format(DATE_TIME_FORMATTER) : null);
        row.put("addressSummary", data.partnerAddressSummary().getOrDefault(partner.getId(), "Não informado"));

        LocalDate contractDate = partner.getContractDate();
        row.put("contractDate", contractDate != null ? contractDate.format(DATE_FORMATTER) : null);

        return row;
    }

    private String resolveVehicleStatus(Vehicle vehicle) {
        if (vehicle.getVehicleStatus() != null) {
            return vehicle.getVehicleStatus().getDisplayName();
        }
        if (vehicle.getStatus() != null) {
            try {
                return VehicleStatus.fromId(vehicle.getStatus()).getDisplayName();
            } catch (IllegalArgumentException ignored) {
                // fallback handled below
            }
        }
        return "Não informado";
    }

    private String resolvePartnerStatus(Partner partner) {
        if (partner.getStatus() != null) {
            return partner.getStatus().getDisplayName();
        }
        return "Não informado";
    }

    private String safeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
