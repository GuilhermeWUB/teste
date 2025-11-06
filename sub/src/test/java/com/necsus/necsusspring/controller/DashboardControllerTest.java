package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.DashboardSummary;
import com.necsus.necsusspring.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
    }

    @Test
    public void testDashboard_ShouldLoadSummaryAndReturnView() throws Exception {
        DashboardSummary summary = new DashboardSummary(100L, 75L, 30L, 70);
        when(dashboardService.loadSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("pageTitle"))
                .andExpect(model().attribute("totalPartners", 100L))
                .andExpect(model().attribute("activeVehicles", 75L))
                .andExpect(model().attribute("pendingInvoices", 30L))
                .andExpect(model().attribute("collectionProgress", 70));

        verify(dashboardService, times(1)).loadSummary();
    }

    @Test
    public void testDashboard_WithZeroValues_ShouldHandleGracefully() throws Exception {
        DashboardSummary summary = new DashboardSummary(0L, 0L, 0L, 0);
        when(dashboardService.loadSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("totalPartners", 0L))
                .andExpect(model().attribute("activeVehicles", 0L))
                .andExpect(model().attribute("pendingInvoices", 0L))
                .andExpect(model().attribute("collectionProgress", 0));

        verify(dashboardService, times(1)).loadSummary();
    }

    @Test
    public void testDashboard_WithHighValues_ShouldDisplayCorrectly() throws Exception {
        DashboardSummary summary = new DashboardSummary(5000L, 4500L, 1000L, 95);
        when(dashboardService.loadSummary()).thenReturn(summary);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("totalPartners", 5000L))
                .andExpect(model().attribute("activeVehicles", 4500L))
                .andExpect(model().attribute("pendingInvoices", 1000L))
                .andExpect(model().attribute("collectionProgress", 95));

        verify(dashboardService, times(1)).loadSummary();
    }
}
