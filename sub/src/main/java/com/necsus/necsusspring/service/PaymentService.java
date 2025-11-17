package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.BankSlip;
import com.necsus.necsusspring.model.BankShipment;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.repository.BankSlipRepository;
import com.necsus.necsusspring.repository.BankShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private BankSlipRepository bankSlipRepository;

    @Autowired
    private BankShipmentRepository bankShipmentRepository;

    @Autowired
    private VehicleService vehicleService;

    @Transactional
    public BankShipment generateMonthlyInvoices(Long vehicleId, int numberOfSlips) {
        Vehicle vehicle = vehicleService.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // Verifica se o veículo tem um pagamento configurado
        if (vehicle.getPayment() == null) {
            throw new IllegalArgumentException("Veículo não possui pagamento configurado. Configure o pagamento antes de gerar faturas.");
        }

        // Verifica se o valor mensal está configurado
        if (vehicle.getPayment().getMonthly() == null || vehicle.getPayment().getMonthly().signum() <= 0) {
            throw new IllegalArgumentException("Valor mensal do pagamento não está configurado corretamente.");
        }

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

        return bankShipment;
    }

    /**
     * Lista todas as faturas com paginação
     */
    @Transactional(readOnly = true)
    public Page<BankSlip> listAllInvoices(Pageable pageable) {
        return bankSlipRepository.findAllOrderByVencimentoDesc(pageable);
    }

    /**
     * Lista faturas pendentes (não pagas)
     */
    @Transactional(readOnly = true)
    public List<BankSlip> listPendingInvoices() {
        return bankSlipRepository.findPendingInvoices();
    }

    /**
     * Lista faturas já pagas
     */
    @Transactional(readOnly = true)
    public List<BankSlip> listPaidInvoices() {
        return bankSlipRepository.findPaidInvoices();
    }

    /**
     * Marca uma fatura como paga (para uso do admin)
     * @param bankSlipId ID da fatura
     * @param valorRecebido Valor recebido (opcional, se null usa o valor da fatura)
     * @return Fatura atualizada
     */
    @Transactional
    public BankSlip markAsPaid(Long bankSlipId, BigDecimal valorRecebido) {
        BankSlip bankSlip = bankSlipRepository.findById(bankSlipId)
                .orElseThrow(() -> new RuntimeException("Fatura não encontrada"));

        // Verifica se já está paga
        if (bankSlip.getStatus() != null && bankSlip.getStatus() == 1) {
            throw new IllegalStateException("Fatura já está marcada como paga");
        }

        // Marca como paga
        bankSlip.setStatus(1);
        bankSlip.setDataPagamento(new Date());

        // Define o valor recebido (se não fornecido, usa o valor da fatura)
        if (valorRecebido != null) {
            bankSlip.setValorRecebido(valorRecebido);
        } else {
            bankSlip.setValorRecebido(bankSlip.getValor());
        }

        return bankSlipRepository.save(bankSlip);
    }

    /**
     * Busca uma fatura por ID
     */
    @Transactional(readOnly = true)
    public BankSlip findById(Long id) {
        return bankSlipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fatura não encontrada"));
    }

    /**
     * Busca faturas por BankShipment
     */
    @Transactional(readOnly = true)
    public List<BankSlip> findInvoicesByBankShipment(Long bankShipmentId) {
        return bankSlipRepository.findByBankShipmentId(bankShipmentId);
    }
}
