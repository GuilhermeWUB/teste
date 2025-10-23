package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.BankSlip;
import com.necsus.necsusspring.model.BankShipment;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.repository.BankSlipRepository;
import com.necsus.necsusspring.repository.BankShipmentRepository;
import com.necsus.necsusspring.repository.PaymentRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BankSlipRepository bankSlipRepository;

    @Autowired
    private BankShipmentRepository bankShipmentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Transactional
    public Partner generateMonthlyInvoices(Long vehicleId, int numberOfSlips) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() -> new RuntimeException("Vehicle not found"));

        BankShipment bankShipment = new BankShipment();
        bankShipment.setVehicle(vehicle);
        bankShipment.setDateCreate(new Date());
        bankShipment.setStatus(0);
        bankShipment = bankShipmentRepository.save(bankShipment);

        List<BankSlip> bankSlips = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < numberOfSlips; i++) {
            BankSlip bankSlip = new BankSlip();
            bankSlip.setPayment(vehicle.getPayment());
            bankSlip.setPartner(vehicle.getPartner());
            calendar.setTime(new Date());
            calendar.add(Calendar.MONTH, i);
            bankSlip.setVencimento(calendar.getTime());
            bankSlip.setValor(vehicle.getPayment().getMonthly());
            bankSlip.setStatus(0);
            bankSlip.setBankShipment(bankShipment);
            bankSlips.add(bankSlip);
        }
        bankSlipRepository.saveAll(bankSlips);

        return vehicle.getPartner();
    }
}
