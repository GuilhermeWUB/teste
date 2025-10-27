package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.DashboardSummary;
import com.necsus.necsusspring.repository.BankSlipRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final PartnerRepository partnerRepository;
    private final VehicleRepository vehicleRepository;
    private final BankSlipRepository bankSlipRepository;

    public DashboardService(PartnerRepository partnerRepository,
                            VehicleRepository vehicleRepository,
                            BankSlipRepository bankSlipRepository) {
        this.partnerRepository = partnerRepository;
        this.vehicleRepository = vehicleRepository;
        this.bankSlipRepository = bankSlipRepository;
    }

    public DashboardSummary loadSummary() {
        long totalPartners = partnerRepository.count();

        long activeVehicles = vehicleRepository.countByStatus(1);
        if (activeVehicles == 0) {
            activeVehicles = vehicleRepository.count();
        }

        long pendingInvoices = bankSlipRepository.countByStatus(0);
        long paidInvoices = bankSlipRepository.countByStatus(1);
        long totalInvoices = pendingInvoices + paidInvoices;
        int collectionProgress = totalInvoices == 0
                ? 0
                : (int) Math.round((double) paidInvoices / totalInvoices * 100);

        return new DashboardSummary(totalPartners, activeVehicles, pendingInvoices, collectionProgress);
    }
}
