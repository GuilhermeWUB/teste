package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.DashboardSummary;
import com.necsus.necsusspring.model.Demand;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.service.DashboardService;
import com.necsus.necsusspring.service.DemandService;
import com.necsus.necsusspring.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private DemandService demandService;

    @Mock
    private UserAccountService userAccountService;

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
                .andExpect(model().attribute("collectionProgress", 70))
                .andExpect(model().attribute("nextDemands", empty()));

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
                .andExpect(model().attribute("collectionProgress", 0))
                .andExpect(model().attribute("nextDemands", empty()));

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
                .andExpect(model().attribute("collectionProgress", 95))
                .andExpect(model().attribute("nextDemands", empty()));

        verify(dashboardService, times(1)).loadSummary();
    }

    @Test
    public void testDashboard_WithAuthenticatedUser_ShouldIncludeNextDemands() throws Exception {
        DashboardSummary summary = new DashboardSummary(10L, 8L, 2L, 50);
        when(dashboardService.loadSummary()).thenReturn(summary);

        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setUsername("john");
        Demand demand = new Demand();
        demand.setId(100L);
        demand.setTitulo("Atualizar contrato");

        when(userAccountService.findByUsername("john")).thenReturn(Optional.of(user));
        when(demandService.findNextDemandsForUser(user, 3)).thenReturn(List.of(demand));

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("john", "password");
        authentication.setAuthenticated(true);

        mockMvc.perform(get("/dashboard").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("nextDemands", hasSize(1)));

        verify(userAccountService, times(1)).findByUsername("john");
        verify(demandService, times(1)).findNextDemandsForUser(user, 3);
    }
}
