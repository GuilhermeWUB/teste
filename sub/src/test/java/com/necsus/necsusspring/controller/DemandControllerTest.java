package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.service.DemandService;
import com.necsus.necsusspring.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class DemandControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DemandService demandService;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DemandController demandController;

    private UserAccount testUser;
    private Demand testDemand;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(demandController).build();

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(RoleType.USER.getCode());

        testDemand = new Demand();
        testDemand.setId(1L);
        testDemand.setTitulo("Test Demand");
        testDemand.setDescricao("Test Description");
        testDemand.setStatus(DemandStatus.PENDENTE);
        testDemand.setCreatedBy(testUser);
    }

    @Test
    public void testIndex_WithAdminRole_ShouldRedirectToDirectorPanel() throws Exception {
        when(authentication.getName()).thenReturn("admin");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(get("/demands").principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demands/director"));
    }

    @Test
    public void testIndex_WithUserRole_ShouldRedirectToHome() throws Exception {
        when(authentication.getName()).thenReturn("user");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(get("/demands").principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    public void testShowDirectorPanel_WithAdminRole_ShouldShowPanel() throws Exception {
        when(authentication.getName()).thenReturn("admin");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(userAccountService.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(demandService.findAll()).thenReturn(Arrays.asList(testDemand));

        mockMvc.perform(get("/demands/director").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("demandas_diretor"))
                .andExpect(model().attributeExists("demands"))
                .andExpect(model().attributeExists("pendentes"))
                .andExpect(model().attributeExists("emAndamento"))
                .andExpect(model().attributeExists("concluidas"));

        verify(demandService, times(1)).findAll();
    }

    @Test
    public void testShowDirectorPanel_WithUserRole_ShouldRedirect() throws Exception {
        when(authentication.getName()).thenReturn("user");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(get("/demands/director").principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demands/my-demands"));

        verify(demandService, never()).findAll();
    }

    @Test
    public void testShowMyDemands_WithUserRole_ShouldBeBlocked() throws Exception {
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(get("/demands/my-demands").principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(demandService, never()).findAccessibleByUser(any(), any());
    }

    @Test
    public void testShowMyDemands_WithOtherRole_ShouldShowDemands() throws Exception {
        UserAccount rhUser = new UserAccount();
        rhUser.setId(3L);
        rhUser.setUsername("rhuser");
        rhUser.setRole(RoleType.RH.getCode());

        when(authentication.getName()).thenReturn("rhuser");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_RH"))
        );
        when(userAccountService.findByUsername("rhuser")).thenReturn(Optional.of(rhUser));
        when(demandService.findAccessibleByUser(rhUser, "RH")).thenReturn(Arrays.asList(testDemand));

        mockMvc.perform(get("/demands/my-demands").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("minhas_demandas"))
                .andExpect(model().attributeExists("demands"));

        verify(demandService, times(1)).findAccessibleByUser(rhUser, "RH");
    }

    @Test
    public void testCreateDemand_WithAdminRole_ShouldCreateSuccessfully() throws Exception {
        UserAccount adminUser = new UserAccount();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRole(RoleType.ADMIN.getCode());

        when(authentication.getName()).thenReturn("admin");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(userAccountService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(demandService.createDemand(any(Demand.class))).thenReturn(testDemand);

        mockMvc.perform(post("/demands/create")
                        .param("title", "New Demand")
                        .param("description", "New Description")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demands/director"));

        verify(demandService, times(1)).createDemand(any(Demand.class));
    }

    @Test
    public void testCreateDemand_WithUserRole_ShouldBeDenied() throws Exception {
        when(authentication.getName()).thenReturn("user");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(post("/demands/create")
                        .param("title", "New Demand")
                        .param("description", "New Description")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demands/my-demands"));

        verify(demandService, never()).createDemand(any());
    }

    @Test
    public void testUpdateStatus_ShouldUpdateDemandStatus() throws Exception {
        when(authentication.getName()).thenReturn("admin");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(userAccountService.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(demandService.findById(1L)).thenReturn(Optional.of(testDemand));
        when(demandService.updateStatus(eq(1L), any(DemandStatus.class))).thenReturn(testDemand);

        mockMvc.perform(post("/demands/1/update-status")
                        .param("status", "CONCLUIDA")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection());

        verify(demandService, times(1)).updateStatus(1L, DemandStatus.CONCLUIDA);
    }

    @Test
    public void testAssignToMe_WithUserRole_ShouldBeBlocked() throws Exception {
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(post("/demands/1/assign-to-me")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(demandService, never()).assignToUser(any(), any());
    }

    @Test
    public void testDeleteDemand_WithAdminRole_ShouldDeleteSuccessfully() throws Exception {
        when(authentication.getName()).thenReturn("admin");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockMvc.perform(post("/demands/1/delete")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demands/director"));

        verify(demandService, times(1)).deleteDemand(1L);
    }

    @Test
    public void testDeleteDemand_WithUserRole_ShouldBeDenied() throws Exception {
        when(authentication.getName()).thenReturn("user");
        when(authentication.getAuthorities()).thenReturn(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(post("/demands/1/delete")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/demands/my-demands"));

        verify(demandService, never()).deleteDemand(any());
    }
}
