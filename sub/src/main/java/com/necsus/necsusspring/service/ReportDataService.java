package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Address;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReportDataService {

    private final PartnerService partnerService;
    private final VehicleService vehicleService;

    public ReportDataService(PartnerService partnerService, VehicleService vehicleService) {
        this.partnerService = partnerService;
        this.vehicleService = vehicleService;
    }

    @Transactional(readOnly = true)
    public PartnerReportData loadPartnerReportData() {
        List<Partner> partners = partnerService.getAllPartners();
        boolean hasPartners = !partners.isEmpty();

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

        return new PartnerReportData(
                partners,
                hasPartners,
                partners.size(),
                partnersWithVehicles,
                partners.size() - partnersWithVehicles,
                totalVehicles,
                partnersByCity,
                averageFormat.format(averageVehicles),
                partnerVehicleCount,
                partnerAddressSummary
        );
    }

    @Transactional(readOnly = true)
    public VehicleReportData loadVehicleReportData() {
        List<Vehicle> vehicles = vehicleService.listAll(null);
        boolean hasVehicles = !vehicles.isEmpty();

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

        BigDecimal totalMonthlyValue = vehicles.stream()
                .map(Vehicle::getPayment)
                .filter(Objects::nonNull)
                .map(payment -> payment.getMonthly() != null ? payment.getMonthly() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        int distinctPartners = vehicles.stream()
                .map(Vehicle::getPartnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size();

        return new VehicleReportData(
                vehicles,
                hasVehicles,
                vehicles.size(),
                distinctPartners,
                totalMonthlyValue,
                currencyFormat.format(totalMonthlyValue),
                vehiclesByFuel,
                vehiclesByMaker
        );
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
                builder.append(" • ");
            }
            builder.append(value);
        }
    }

    private String combineAddressAndNumber(String address, String number) {
        if (isBlank(address)) {
            return safeValue(number);
        }
        if (isBlank(number)) {
            return address.trim();
        }
        return address.trim() + ", " + number.trim();
    }

    private String combineCityAndState(String city, String state) {
        if (isBlank(city)) {
            return safeValue(state);
        }
        if (isBlank(state)) {
            return city.trim();
        }
        return city.trim() + " - " + state.trim();
    }

    private String safeValue(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record PartnerReportData(
            List<Partner> partners,
            boolean hasPartners,
            long totalPartners,
            long partnersWithVehicles,
            long partnersWithoutVehicles,
            long totalVehicles,
            Map<String, Long> partnersByCity,
            String averageVehiclesPerPartner,
            Map<Long, Integer> partnerVehicleCount,
            Map<Long, String> partnerAddressSummary
    ) {
    }

    public record VehicleReportData(
            List<Vehicle> vehicles,
            boolean hasVehicles,
            long totalVehicles,
            long distinctPartners,
            BigDecimal totalMonthlyValue,
            String totalMonthlyValueFormatted,
            Map<String, Long> vehiclesByFuel,
            Map<String, Long> vehiclesByMaker
    ) {
    }
}

