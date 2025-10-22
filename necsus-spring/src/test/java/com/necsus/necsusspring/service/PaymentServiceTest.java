package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.BankShipment;
import com.necsus.necsusspring.model.BankSlip;
import com.necsus.necsusspring.model.Payment;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.repository.BankShipmentRepository;
import com.necsus.necsusspring.repository.BankSlipRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private BankSlipRepository bankSlipRepository;

    @Mock
    private BankShipmentRepository bankShipmentRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Test
    public void testGenerateMonthlyInvoices() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        Payment payment = new Payment();
        payment.setMonthly(new BigDecimal("100.00"));
        vehicle.setPayment(payment);

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(bankShipmentRepository.save(any(BankShipment.class))).thenAnswer(i -> i.getArguments()[0]);

        paymentService.generateMonthlyInvoices(1L, 12);

        verify(bankShipmentRepository, times(1)).save(any(BankShipment.class));
        verify(bankSlipRepository, times(1)).saveAll(any());
    }
}