package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.LegalProcessRequest;
import com.necsus.necsusspring.model.LegalProcess;
import com.necsus.necsusspring.model.LegalProcessStatus;
import com.necsus.necsusspring.model.LegalProcessType;
import com.necsus.necsusspring.repository.LegalProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalProcessServiceTest {

    @Mock
    private LegalProcessRepository legalProcessRepository;

    private LegalProcessService legalProcessService;

    @BeforeEach
    void setUp() {
        legalProcessService = new LegalProcessService(legalProcessRepository);
        when(legalProcessRepository.save(any(LegalProcess.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createShouldAssignDefaultFidelityStatus() {
        LegalProcessRequest request = new LegalProcessRequest(
                "Autor Fidelidade",
                "Réu",
                "Matéria",
                "PROC-FID-001",
                BigDecimal.TEN,
                "Pedidos",
                LegalProcessType.FIDELIDADE,
                null,
                null
        );

        LegalProcess result = legalProcessService.create(request);

        ArgumentCaptor<LegalProcess> captor = ArgumentCaptor.forClass(LegalProcess.class);
        verify(legalProcessRepository).save(captor.capture());
        LegalProcess saved = captor.getValue();

        assertThat(saved.getProcessType()).isEqualTo(LegalProcessType.FIDELIDADE);
        assertThat(saved.getStatus()).isEqualTo(LegalProcessStatus.FIDELIDADE_EM_ABERTO);
        assertThat(result.getProcessType()).isEqualTo(LegalProcessType.FIDELIDADE);
        assertThat(result.getStatus()).isEqualTo(LegalProcessStatus.FIDELIDADE_EM_ABERTO);
    }

    @Test
    void createShouldAssignDefaultTrackerStatus() {
        LegalProcessRequest request = new LegalProcessRequest(
                "Autor Rastreador",
                "Réu",
                "Matéria",
                "PROC-RAST-001",
                BigDecimal.ONE,
                "Pedidos",
                LegalProcessType.RASTREADOR,
                null,
                null
        );

        LegalProcess result = legalProcessService.create(request);

        ArgumentCaptor<LegalProcess> captor = ArgumentCaptor.forClass(LegalProcess.class);
        verify(legalProcessRepository).save(captor.capture());
        LegalProcess saved = captor.getValue();

        assertThat(saved.getProcessType()).isEqualTo(LegalProcessType.RASTREADOR);
        assertThat(saved.getStatus()).isEqualTo(LegalProcessStatus.RASTREADOR_EM_ABERTO);
        assertThat(result.getProcessType()).isEqualTo(LegalProcessType.RASTREADOR);
        assertThat(result.getStatus()).isEqualTo(LegalProcessStatus.RASTREADOR_EM_ABERTO);
    }
}
