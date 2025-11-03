package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.PartnerStatus;
import com.necsus.necsusspring.service.FileStorageService;
import com.necsus.necsusspring.service.PartnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
public class PartnerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PartnerService partnerService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PartnerController partnerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(partnerController).build();
    }

    @Test
    public void testUpdatePartner_shouldPreserveExistingData() throws Exception {
        Long partnerId = 1L;
        LocalDateTime registrationDate = LocalDateTime.now().minusDays(10);
        Partner existingPartner = new Partner();
        existingPartner.setId(partnerId);
        existingPartner.setName("Old Name");
        existingPartner.setStatus(PartnerStatus.ATIVO);
        existingPartner.setRegistrationDate(registrationDate);

        when(partnerService.getPartnerById(partnerId)).thenReturn(Optional.of(existingPartner));

        mockMvc.perform(post("/partners/update/{id}", partnerId)
                .param("name", "New Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/partners"));

        verify(partnerService).updatePartner(argThat(partner ->
                "New Name".equals(partner.getName()) &&
                PartnerStatus.ATIVO.equals(partner.getStatus()) &&
                registrationDate.equals(partner.getRegistrationDate())
        ));
    }
}
