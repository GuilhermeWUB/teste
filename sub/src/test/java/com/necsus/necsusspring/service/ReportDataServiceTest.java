package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Address;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Payment;
import com.necsus.necsusspring.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportDataServiceTest {

    @InjectMocks
    private ReportDataService reportDataService;

    @Mock
    private PartnerService partnerService;

    @Mock
    private VehicleService vehicleService;

    private Partner testPartner1;
    private Partner testPartner2;
    private Vehicle testVehicle1;
    private Vehicle testVehicle2;
    private Address testAddress;
    private Payment testPayment;

    @BeforeEach
    public void setUp() {
        testAddress = new Address();
        testAddress.setAddress("Test Street");
        testAddress.setNumber("123");
        testAddress.setNeighborhood("Test Neighborhood");
        testAddress.setCity("São Paulo");
        testAddress.setStates("SP");
        testAddress.setZipcode("01234-567");

        testPayment = new Payment();
        testPayment.setMonthly(new BigDecimal("150.00"));

        testPartner1 = new Partner();
        testPartner1.setId(1L);
        testPartner1.setName("Partner 1");
        testPartner1.setAddress(testAddress);
        testPartner1.setVehicles(new ArrayList<>());

        testPartner2 = new Partner();
        testPartner2.setId(2L);
        testPartner2.setName("Partner 2");
        testPartner2.setVehicles(new ArrayList<>());

        testVehicle1 = new Vehicle();
        testVehicle1.setId(1L);
        testVehicle1.setPlaque("ABC1234");
        testVehicle1.setMaker("Toyota");
        testVehicle1.setTipo_combustivel("Gasolina");
        testVehicle1.setPartnerId(1L);
        testVehicle1.setPayment(testPayment);

        testVehicle2 = new Vehicle();
        testVehicle2.setId(2L);
        testVehicle2.setPlaque("XYZ5678");
        testVehicle2.setMaker("Honda");
        testVehicle2.setTipo_combustivel("Flex");
        testVehicle2.setPartnerId(1L);
        testVehicle2.setPayment(testPayment);

        testPartner1.getVehicles().add(testVehicle1);
        testPartner1.getVehicles().add(testVehicle2);
    }

    @Test
    public void testLoadPartnerReportData_WithPartners_ShouldReturnCompleteReport() {
        List<Partner> partners = Arrays.asList(testPartner1, testPartner2);
        when(partnerService.getAllPartners()).thenReturn(partners);

        ReportDataService.PartnerReportData result = reportDataService.loadPartnerReportData();

        assertNotNull(result);
        assertTrue(result.hasPartners());
        assertEquals(2, result.totalPartners());
        assertEquals(1, result.partnersWithVehicles());
        assertEquals(1, result.partnersWithoutVehicles());
        assertEquals(2, result.totalVehicles());
        assertNotNull(result.partnersByCity());
        assertNotNull(result.averageVehiclesPerPartner());
        verify(partnerService, times(1)).getAllPartners();
    }

    @Test
    public void testLoadPartnerReportData_WithNoPartners_ShouldReturnEmptyReport() {
        when(partnerService.getAllPartners()).thenReturn(new ArrayList<>());

        ReportDataService.PartnerReportData result = reportDataService.loadPartnerReportData();

        assertNotNull(result);
        assertFalse(result.hasPartners());
        assertEquals(0, result.totalPartners());
        assertEquals(0, result.partnersWithVehicles());
        assertEquals(0, result.partnersWithoutVehicles());
        assertEquals(0, result.totalVehicles());
        assertEquals("0,00", result.averageVehiclesPerPartner());
    }

    @Test
    public void testLoadPartnerReportData_ShouldGroupPartnersByCity() {
        Address address2 = new Address();
        address2.setCity("Rio de Janeiro");
        testPartner2.setAddress(address2);

        List<Partner> partners = Arrays.asList(testPartner1, testPartner2);
        when(partnerService.getAllPartners()).thenReturn(partners);

        ReportDataService.PartnerReportData result = reportDataService.loadPartnerReportData();

        assertNotNull(result.partnersByCity());
        assertEquals(2, result.partnersByCity().size());
        assertTrue(result.partnersByCity().containsKey("São Paulo"));
        assertTrue(result.partnersByCity().containsKey("Rio de Janeiro"));
    }

    @Test
    public void testLoadPartnerReportData_ShouldCalculateAverageVehicles() {
        List<Partner> partners = Arrays.asList(testPartner1, testPartner2);
        when(partnerService.getAllPartners()).thenReturn(partners);

        ReportDataService.PartnerReportData result = reportDataService.loadPartnerReportData();

        // Partner 1 has 2 vehicles, Partner 2 has 0 vehicles
        // Average = 2 / 2 = 1.00
        assertNotNull(result.averageVehiclesPerPartner());
        assertTrue(result.averageVehiclesPerPartner().contains("1"));
    }

    @Test
    public void testLoadPartnerReportData_ShouldIncludeVehicleCount() {
        List<Partner> partners = Arrays.asList(testPartner1, testPartner2);
        when(partnerService.getAllPartners()).thenReturn(partners);

        ReportDataService.PartnerReportData result = reportDataService.loadPartnerReportData();

        Map<Long, Integer> vehicleCount = result.partnerVehicleCount();
        assertNotNull(vehicleCount);
        assertEquals(2, vehicleCount.get(1L));
        assertEquals(0, vehicleCount.get(2L));
    }

    @Test
    public void testLoadPartnerReportData_ShouldFormatAddressSummary() {
        List<Partner> partners = Arrays.asList(testPartner1);
        when(partnerService.getAllPartners()).thenReturn(partners);

        ReportDataService.PartnerReportData result = reportDataService.loadPartnerReportData();

        Map<Long, String> addressSummary = result.partnerAddressSummary();
        assertNotNull(addressSummary);
        String address = addressSummary.get(1L);
        assertNotNull(address);
        assertTrue(address.contains("Test Street"));
        assertTrue(address.contains("123"));
        assertTrue(address.contains("São Paulo"));
        assertTrue(address.contains("SP"));
        assertTrue(address.contains("01234-567"));
    }

    @Test
    public void testLoadVehicleReportData_WithVehicles_ShouldReturnCompleteReport() {
        List<Vehicle> vehicles = Arrays.asList(testVehicle1, testVehicle2);
        when(vehicleService.listAll(null)).thenReturn(vehicles);

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        assertNotNull(result);
        assertTrue(result.hasVehicles());
        assertEquals(2, result.totalVehicles());
        assertEquals(1, result.distinctPartners());
        assertEquals(new BigDecimal("300.00"), result.totalMonthlyValue());
        assertNotNull(result.totalMonthlyValueFormatted());
        verify(vehicleService, times(1)).listAll(null);
    }

    @Test
    public void testLoadVehicleReportData_WithNoVehicles_ShouldReturnEmptyReport() {
        when(vehicleService.listAll(null)).thenReturn(new ArrayList<>());

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        assertNotNull(result);
        assertFalse(result.hasVehicles());
        assertEquals(0, result.totalVehicles());
        assertEquals(0, result.distinctPartners());
        assertEquals(BigDecimal.ZERO, result.totalMonthlyValue());
    }

    @Test
    public void testLoadVehicleReportData_ShouldGroupByFuelType() {
        List<Vehicle> vehicles = Arrays.asList(testVehicle1, testVehicle2);
        when(vehicleService.listAll(null)).thenReturn(vehicles);

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        Map<String, Long> vehiclesByFuel = result.vehiclesByFuel();
        assertNotNull(vehiclesByFuel);
        assertEquals(2, vehiclesByFuel.size());
        assertEquals(1L, vehiclesByFuel.get("Gasolina"));
        assertEquals(1L, vehiclesByFuel.get("Flex"));
    }

    @Test
    public void testLoadVehicleReportData_ShouldGroupByMaker() {
        List<Vehicle> vehicles = Arrays.asList(testVehicle1, testVehicle2);
        when(vehicleService.listAll(null)).thenReturn(vehicles);

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        Map<String, Long> vehiclesByMaker = result.vehiclesByMaker();
        assertNotNull(vehiclesByMaker);
        assertEquals(2, vehiclesByMaker.size());
        assertEquals(1L, vehiclesByMaker.get("Toyota"));
        assertEquals(1L, vehiclesByMaker.get("Honda"));
    }

    @Test
    public void testLoadVehicleReportData_WithNullFuelType_ShouldHandleGracefully() {
        testVehicle1.setTipo_combustivel(null);
        List<Vehicle> vehicles = Arrays.asList(testVehicle1, testVehicle2);
        when(vehicleService.listAll(null)).thenReturn(vehicles);

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        Map<String, Long> vehiclesByFuel = result.vehiclesByFuel();
        assertNotNull(vehiclesByFuel);
        assertTrue(vehiclesByFuel.containsKey("Não informado"));
    }

    @Test
    public void testLoadVehicleReportData_WithNullMaker_ShouldHandleGracefully() {
        testVehicle1.setMaker(null);
        List<Vehicle> vehicles = Arrays.asList(testVehicle1, testVehicle2);
        when(vehicleService.listAll(null)).thenReturn(vehicles);

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        Map<String, Long> vehiclesByMaker = result.vehiclesByMaker();
        assertNotNull(vehiclesByMaker);
        assertTrue(vehiclesByMaker.containsKey("Não informado"));
    }

    @Test
    public void testLoadVehicleReportData_ShouldCalculateTotalMonthlyValue() {
        testVehicle1.setPayment(testPayment);
        Payment payment2 = new Payment();
        payment2.setMonthly(new BigDecimal("250.00"));
        testVehicle2.setPayment(payment2);

        List<Vehicle> vehicles = Arrays.asList(testVehicle1, testVehicle2);
        when(vehicleService.listAll(null)).thenReturn(vehicles);

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        assertEquals(new BigDecimal("400.00"), result.totalMonthlyValue());
    }

    @Test
    public void testLoadVehicleReportData_WithNullPayment_ShouldHandleGracefully() {
        testVehicle1.setPayment(null);
        List<Vehicle> vehicles = Arrays.asList(testVehicle1, testVehicle2);
        when(vehicleService.listAll(null)).thenReturn(vehicles);

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        assertEquals(new BigDecimal("150.00"), result.totalMonthlyValue());
    }

    @Test
    public void testLoadVehicleReportData_ShouldCountDistinctPartners() {
        testVehicle2.setPartnerId(2L);
        List<Vehicle> vehicles = Arrays.asList(testVehicle1, testVehicle2);
        when(vehicleService.listAll(null)).thenReturn(vehicles);

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        assertEquals(2, result.distinctPartners());
    }

    @Test
    public void testLoadPartnerReportData_WithPartnerWithoutAddress_ShouldHandleGracefully() {
        testPartner1.setAddress(null);
        List<Partner> partners = Arrays.asList(testPartner1);
        when(partnerService.getAllPartners()).thenReturn(partners);

        ReportDataService.PartnerReportData result = reportDataService.loadPartnerReportData();

        Map<Long, String> addressSummary = result.partnerAddressSummary();
        assertNotNull(addressSummary);
        assertEquals("Não informado", addressSummary.get(1L));
    }

    @Test
    public void testLoadVehicleReportData_ShouldFormatCurrencyInBrazilianFormat() {
        List<Vehicle> vehicles = Arrays.asList(testVehicle1);
        when(vehicleService.listAll(null)).thenReturn(vehicles);

        ReportDataService.VehicleReportData result = reportDataService.loadVehicleReportData();

        String formatted = result.totalMonthlyValueFormatted();
        assertNotNull(formatted);
        assertTrue(formatted.contains("150") || formatted.contains("R$"));
    }
}
