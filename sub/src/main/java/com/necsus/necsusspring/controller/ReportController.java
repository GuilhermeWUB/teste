package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Address;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final PartnerService partnerService;
    private final VehicleService vehicleService;

    public ReportController(PartnerService partnerService, VehicleService vehicleService) {
        this.partnerService = partnerService;
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public String redirectToDefault() {
        return "redirect:/reports/vehicles";
    }

    @GetMapping("/vehicles")
    public String vehicleReport(Model model) {
        List<Vehicle> vehicles = vehicleService.listAll(null);
        boolean hasVehicles = !vehicles.isEmpty();

        model.addAttribute("pageTitle", "SUB - Relatório de Veículos");
        model.addAttribute("hasVehicles", hasVehicles);
        model.addAttribute("vehicles", vehicles);

        if (!hasVehicles) {
            model.addAttribute("errorMessage", "Não há veículos cadastrados para gerar o relatório.");
            return "relatorio_veiculos";
        }

        model.addAttribute("totalVehicles", vehicles.size());
        model.addAttribute("distinctPartners", vehicles.stream()
                .map(Vehicle::getPartnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size());

        BigDecimal totalMonthlyValue = vehicles.stream()
                .map(Vehicle::getPayment)
                .filter(Objects::nonNull)
                .map(payment -> payment.getMonthly() != null ? payment.getMonthly() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        model.addAttribute("totalMonthlyValue", totalMonthlyValue);
        model.addAttribute("totalMonthlyValueFormatted", currencyFormat.format(totalMonthlyValue));

        Map<String, Long> vehiclesByFuel = sortByValueDesc(vehicles.stream()
                .collect(Collectors.groupingBy(
                        vehicle -> normalizeText(vehicle.getTipo_combustivel(), "Não informado"),
                        Collectors.counting()
                )));

        Map<String, Long> vehiclesByMaker = sortByValueDesc(vehicles.stream()
                .collect(Collectors.groupingBy(
                        vehicle -> normalizeText(vehicle.getMaker(), "Não informado"),
                        Collectors.counting()
                )));

        model.addAttribute("vehiclesByFuel", vehiclesByFuel);
        model.addAttribute("vehiclesByMaker", vehiclesByMaker);

        return "relatorio_veiculos";
    }

    @GetMapping("/partners")
    public String partnerReport(Model model) {
        List<Partner> partners = partnerService.getAllPartners();
        boolean hasPartners = !partners.isEmpty();

        model.addAttribute("pageTitle", "SUB - Relatório de Associados");
        model.addAttribute("hasPartners", hasPartners);
        model.addAttribute("partners", partners);

        if (!hasPartners) {
            model.addAttribute("errorMessage", "Não há associados cadastrados para gerar o relatório.");
            return "relatorio_associados";
        }

        long partnersWithVehicles = partners.stream()
                .filter(partner -> partner.getVehicles() != null && !partner.getVehicles().isEmpty())
                .count();

        long totalVehicles = partners.stream()
                .mapToLong(partner -> partner.getVehicles() != null ? partner.getVehicles().size() : 0)
                .sum();

        Map<String, Long> partnersByCity = sortByValueDesc(partners.stream()
                .map(Partner::getAddress)
                .filter(Objects::nonNull)
                .map(Address::getCity)
                .filter(city -> city != null && !city.isBlank())
                .collect(Collectors.groupingBy(this::normalizeCity, Collectors.counting())));

        double averageVehicles = partners.isEmpty() ? 0d : (double) totalVehicles / partners.size();
        NumberFormat averageFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        averageFormat.setMinimumFractionDigits(2);
        averageFormat.setMaximumFractionDigits(2);

        Map<Long, Integer> partnerVehicleCount = partners.stream()
                .collect(Collectors.toMap(
                        Partner::getId,
                        partner -> partner.getVehicles() != null ? partner.getVehicles().size() : 0,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        Map<Long, String> partnerAddressSummary = partners.stream()
                .collect(Collectors.toMap(
                        Partner::getId,
                        partner -> formatFullAddress(partner.getAddress()),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        model.addAttribute("totalPartners", partners.size());
        model.addAttribute("partnersWithVehicles", partnersWithVehicles);
        model.addAttribute("partnersWithoutVehicles", partners.size() - partnersWithVehicles);
        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("partnersByCity", partnersByCity);
        model.addAttribute("averageVehiclesPerPartner", averageFormat.format(averageVehicles));
        model.addAttribute("partnerVehicleCount", partnerVehicleCount);
        model.addAttribute("partnerAddressSummary", partnerAddressSummary);

        return "relatorio_associados";
    }

    private Map<String, Long> sortByValueDesc(Map<String, Long> source) {
        return source.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }

    private String normalizeText(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String normalizeCity(String city) {
        return normalizeText(city, "Não informado");
    }

    private String formatFullAddress(Address address) {
        if (address == null) {
            return "Não informado";
        }

        String main = combineAddressAndNumber(address.getAddress(), address.getNumber());
        String neighborhood = safeValue(address.getNeighborhood());
        String cityState = combineCityAndState(address.getCity(), address.getStates());
        String zipcode = safeValue(address.getZipcode());

        StringBuilder builder = new StringBuilder();

        appendPart(builder, main);
        appendPart(builder, neighborhood);
        appendPart(builder, cityState);

        if (!isBlank(zipcode)) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append("CEP ").append(zipcode);
        }

        return builder.length() > 0 ? builder.toString() : "Não informado";
    }

    private void appendPart(StringBuilder builder, String value) {
        if (!isBlank(value)) {
            if (builder.length() > 0) {
                builder.append(" - ");
            }
            builder.append(value);
        }
    }

    private String combineAddressAndNumber(String address, String number) {
        if (isBlank(address) && isBlank(number)) {
            return null;
        }
        if (isBlank(number)) {
            return address != null ? address.trim() : null;
        }
        if (isBlank(address)) {
            return number.trim();
        }
        return address.trim() + ", " + number.trim();
    }

    private String combineCityAndState(String city, String state) {
        if (isBlank(city) && isBlank(state)) {
            return null;
        }
        if (isBlank(state)) {
            return city != null ? city.trim() : null;
        }
        if (isBlank(city)) {
            return state.trim();
        }
        return city.trim() + "/" + state.trim();
    }

    private String safeValue(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

