package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.DashboardSummary;
import com.necsus.necsusspring.repository.BankSlipRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private BankSlipRepository bankSlipRepository;

    @Test
    public void testLoadSummary_WithActiveVehicles_ShouldReturnCorrectSummary() {
        when(partnerRepository.count()).thenReturn(100L);
        when(vehicleRepository.countByStatus(1)).thenReturn(75L);
        when(bankSlipRepository.countByStatus(0)).thenReturn(30L);
        when(bankSlipRepository.countByStatus(1)).thenReturn(70L);

        DashboardSummary result = dashboardService.loadSummary();

        assertNotNull(result);
        assertEquals(100L, result.totalPartners());
        assertEquals(75L, result.activeVehicles());
        assertEquals(30L, result.pendingInvoices());
        assertEquals(70, result.collectionProgress()); // 70 out of 100 = 70%
        verify(partnerRepository, times(1)).count();
        verify(vehicleRepository, times(1)).countByStatus(1);
        verify(bankSlipRepository, times(1)).countByStatus(0);
        verify(bankSlipRepository, times(1)).countByStatus(1);
    }

    @Test
    public void testLoadSummary_WithNoActiveVehicles_ShouldUseAllVehicles() {
        when(partnerRepository.count()).thenReturn(50L);
        when(vehicleRepository.countByStatus(1)).thenReturn(0L);
        when(vehicleRepository.count()).thenReturn(40L);
        when(bankSlipRepository.countByStatus(0)).thenReturn(10L);
        when(bankSlipRepository.countByStatus(1)).thenReturn(20L);

        DashboardSummary result = dashboardService.loadSummary();

        assertNotNull(result);
        assertEquals(50L, result.totalPartners());
        assertEquals(40L, result.activeVehicles());
        assertEquals(10L, result.pendingInvoices());
        assertEquals(67, result.collectionProgress()); // 20 out of 30 = 66.67% rounded to 67%
        verify(vehicleRepository, times(1)).count();
    }

    @Test
    public void testLoadSummary_WithNoInvoices_ShouldReturn0CollectionProgress() {
        when(partnerRepository.count()).thenReturn(25L);
        when(vehicleRepository.countByStatus(1)).thenReturn(20L);
        when(bankSlipRepository.countByStatus(0)).thenReturn(0L);
        when(bankSlipRepository.countByStatus(1)).thenReturn(0L);

        DashboardSummary result = dashboardService.loadSummary();

        assertNotNull(result);
        assertEquals(25L, result.totalPartners());
        assertEquals(20L, result.activeVehicles());
        assertEquals(0L, result.pendingInvoices());
        assertEquals(0, result.collectionProgress());
    }

    @Test
    public void testLoadSummary_With100PercentCollection_ShouldReturn100() {
        when(partnerRepository.count()).thenReturn(10L);
        when(vehicleRepository.countByStatus(1)).thenReturn(10L);
        when(bankSlipRepository.countByStatus(0)).thenReturn(0L);
        when(bankSlipRepository.countByStatus(1)).thenReturn(100L);

        DashboardSummary result = dashboardService.loadSummary();

        assertNotNull(result);
        assertEquals(100, result.collectionProgress());
    }

    @Test
    public void testLoadSummary_WithZeroData_ShouldHandleGracefully() {
        when(partnerRepository.count()).thenReturn(0L);
        when(vehicleRepository.countByStatus(1)).thenReturn(0L);
        when(vehicleRepository.count()).thenReturn(0L);
        when(bankSlipRepository.countByStatus(0)).thenReturn(0L);
        when(bankSlipRepository.countByStatus(1)).thenReturn(0L);

        DashboardSummary result = dashboardService.loadSummary();

        assertNotNull(result);
        assertEquals(0L, result.totalPartners());
        assertEquals(0L, result.activeVehicles());
        assertEquals(0L, result.pendingInvoices());
        assertEquals(0, result.collectionProgress());
    }

    @Test
    public void testLoadSummary_CollectionProgressRounding_ShouldRoundCorrectly() {
        when(partnerRepository.count()).thenReturn(10L);
        when(vehicleRepository.countByStatus(1)).thenReturn(10L);
        when(bankSlipRepository.countByStatus(0)).thenReturn(2L);
        when(bankSlipRepository.countByStatus(1)).thenReturn(1L);

        DashboardSummary result = dashboardService.loadSummary();

        // 1 out of 3 = 33.33% should round to 33%
        assertEquals(33, result.collectionProgress());
    }
}
