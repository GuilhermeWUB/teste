package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class BoletoServiceTest {

    @InjectMocks
    private BoletoService boletoService;

    private BankSlip testBankSlip;
    private Partner testPartner;
    private Vehicle testVehicle;
    private Payment testPayment;
    private Company testCompany;
    private BankAccount testBankAccount;
    private BankAgency testBankAgency;
    private SlipsBriefing testSlipsBriefing;

    @BeforeEach
    public void setUp() {
        testCompany = new Company();
        testCompany.setId(1L);
        testCompany.setCompanyName("Test Company LTDA");

        testPartner = new Partner();
        testPartner.setId(1L);
        testPartner.setName("Test Partner");
        testPartner.setCpf("12345678900");
        testPartner.setCompany(testCompany);

        testVehicle = new Vehicle();
        testVehicle.setId(1L);
        testVehicle.setPlaque("ABC1234");
        testVehicle.setPartner(testPartner);

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setMonthly(new BigDecimal("150.00"));
        testPayment.setVehicle(testVehicle);

        testBankAgency = new BankAgency();
        testBankAgency.setId(1L);
        testBankAgency.setAgencyCode("1234");

        testBankAccount = new BankAccount();
        testBankAccount.setId(1L);
        testBankAccount.setAccountCode("56789");
        testBankAccount.setBankAgency(testBankAgency);

        testSlipsBriefing = new SlipsBriefing();
        testSlipsBriefing.setId(1L);
        testSlipsBriefing.setBankAccount(testBankAccount);

        testBankSlip = new BankSlip();
        testBankSlip.setId(1L);
        testBankSlip.setNumeroDocumento("DOC123");
        testBankSlip.setNossoNumero("789456123");
        testBankSlip.setValor(new BigDecimal("150.00"));
        testBankSlip.setDataDocumento(new Date());
        testBankSlip.setDataProcessamento(new Date());
        testBankSlip.setVencimento(new Date());
        testBankSlip.setPartner(testPartner);
        testBankSlip.setPayment(testPayment);
        testBankSlip.setSlipsBriefing(testSlipsBriefing);
    }

    @Test
    public void testGenerateBoleto_WithValidBankSlip_ShouldReturnPdfBytes() throws IOException {
        byte[] result = boletoService.generateBoleto(testBankSlip);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void testGenerateBoleto_WithNullBankSlip_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            boletoService.generateBoleto(null);
        });

        assertEquals("BankSlip cannot be null", exception.getMessage());
    }

    @Test
    public void testGenerateBoleto_WithMinimalData_ShouldNotThrowException() throws IOException {
        BankSlip minimalSlip = new BankSlip();
        minimalSlip.setValor(new BigDecimal("100.00"));
        minimalSlip.setDataDocumento(new Date());
        minimalSlip.setVencimento(new Date());

        assertDoesNotThrow(() -> {
            byte[] result = boletoService.generateBoleto(minimalSlip);
            assertNotNull(result);
        });
    }

    @Test
    public void testGenerateBoleto_WithoutPartner_ShouldNotThrowException() throws IOException {
        testBankSlip.setPartner(null);

        assertDoesNotThrow(() -> {
            byte[] result = boletoService.generateBoleto(testBankSlip);
            assertNotNull(result);
        });
    }

    @Test
    public void testGenerateBoleto_WithoutPayment_ShouldNotThrowException() throws IOException {
        testBankSlip.setPayment(null);

        assertDoesNotThrow(() -> {
            byte[] result = boletoService.generateBoleto(testBankSlip);
            assertNotNull(result);
        });
    }

    @Test
    public void testGenerateBoleto_WithoutSlipsBriefing_ShouldNotThrowException() throws IOException {
        testBankSlip.setSlipsBriefing(null);

        assertDoesNotThrow(() -> {
            byte[] result = boletoService.generateBoleto(testBankSlip);
            assertNotNull(result);
        });
    }

    @Test
    public void testGenerateBoleto_WithCompleteData_ShouldIncludeAllInformation() throws IOException {
        byte[] result = boletoService.generateBoleto(testBankSlip);

        assertNotNull(result);
        assertTrue(result.length > 0);
        // PDF should be larger when it contains more information
        assertTrue(result.length > 1000, "PDF should contain substantial content");
    }
}
