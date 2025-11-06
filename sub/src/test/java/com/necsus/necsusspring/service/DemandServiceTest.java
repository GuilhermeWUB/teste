package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Demand;
import com.necsus.necsusspring.model.DemandStatus;
import com.necsus.necsusspring.model.RoleType;
import com.necsus.necsusspring.model.UserAccount;
import com.necsus.necsusspring.repository.DemandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DemandServiceTest {

    @InjectMocks
    private DemandService demandService;

    @Mock
    private DemandRepository demandRepository;

    private Demand testDemand;
    private UserAccount testUser;

    @BeforeEach
    public void setUp() {
        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testDemand = new Demand();
        testDemand.setId(1L);
        testDemand.setTitle("Test Demand");
        testDemand.setDescription("Test Description");
        testDemand.setStatus(DemandStatus.ABERTA);
        testDemand.setCreatedBy(testUser);
        testDemand.setCreatedAt(LocalDateTime.now());
    }

    @Test
    public void testCreateDemand_ShouldReturnSavedDemand() {
        when(demandRepository.save(any(Demand.class))).thenReturn(testDemand);

        Demand result = demandService.createDemand(testDemand);

        assertNotNull(result);
        assertEquals("Test Demand", result.getTitle());
        assertEquals(DemandStatus.ABERTA, result.getStatus());
        verify(demandRepository, times(1)).save(testDemand);
    }

    @Test
    public void testUpdateDemand_ShouldReturnUpdatedDemand() {
        testDemand.setTitle("Updated Demand");
        when(demandRepository.save(any(Demand.class))).thenReturn(testDemand);

        Demand result = demandService.updateDemand(testDemand);

        assertNotNull(result);
        assertEquals("Updated Demand", result.getTitle());
        verify(demandRepository, times(1)).save(testDemand);
    }

    @Test
    public void testFindById_WhenDemandExists_ShouldReturnDemand() {
        when(demandRepository.findById(1L)).thenReturn(Optional.of(testDemand));

        Optional<Demand> result = demandService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(testDemand.getId(), result.get().getId());
        verify(demandRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_WhenDemandDoesNotExist_ShouldReturnEmpty() {
        when(demandRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Demand> result = demandService.findById(999L);

        assertFalse(result.isPresent());
        verify(demandRepository, times(1)).findById(999L);
    }

    @Test
    public void testFindAll_ShouldReturnAllDemands() {
        List<Demand> demands = Arrays.asList(testDemand, new Demand());
        when(demandRepository.findAll()).thenReturn(demands);

        List<Demand> result = demandService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(demandRepository, times(1)).findAll();
    }

    @Test
    public void testFindByCreator_ShouldReturnDemandsCreatedByUser() {
        List<Demand> demands = Arrays.asList(testDemand);
        when(demandRepository.findByCreatedByOrderByCreatedAtDesc(testUser)).thenReturn(demands);

        List<Demand> result = demandService.findByCreator(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0).getCreatedBy());
        verify(demandRepository, times(1)).findByCreatedByOrderByCreatedAtDesc(testUser);
    }

    @Test
    public void testFindByAssignedTo_ShouldReturnDemandsAssignedToUser() {
        testDemand.setAssignedTo(testUser);
        List<Demand> demands = Arrays.asList(testDemand);
        when(demandRepository.findByAssignedToOrderByCreatedAtDesc(testUser)).thenReturn(demands);

        List<Demand> result = demandService.findByAssignedTo(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0).getAssignedTo());
        verify(demandRepository, times(1)).findByAssignedToOrderByCreatedAtDesc(testUser);
    }

    @Test
    public void testFindByStatus_ShouldReturnDemandsWithGivenStatus() {
        List<Demand> demands = Arrays.asList(testDemand);
        when(demandRepository.findByStatusOrderByCreatedAtDesc(DemandStatus.ABERTA)).thenReturn(demands);

        List<Demand> result = demandService.findByStatus(DemandStatus.ABERTA);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(DemandStatus.ABERTA, result.get(0).getStatus());
        verify(demandRepository, times(1)).findByStatusOrderByCreatedAtDesc(DemandStatus.ABERTA);
    }

    @Test
    public void testFindByTargetRole_ShouldReturnDemandsForRole() {
        String targetRole = "ADMIN";
        testDemand.setTargetRoles(targetRole);
        List<Demand> demands = Arrays.asList(testDemand);
        when(demandRepository.findByTargetRolesContaining(targetRole)).thenReturn(demands);

        List<Demand> result = demandService.findByTargetRole(targetRole);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(demandRepository, times(1)).findByTargetRolesContaining(targetRole);
    }

    @Test
    public void testFindAccessibleByUser_WhenUserIsAdmin_ShouldReturnAllDemands() {
        List<Demand> allDemands = Arrays.asList(testDemand, new Demand());
        when(demandRepository.findAll()).thenReturn(allDemands);

        List<Demand> result = demandService.findAccessibleByUser(testUser, "ADMIN");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(demandRepository, times(1)).findAll();
        verify(demandRepository, never()).findAccessibleByUser(any(), any());
    }

    @Test
    public void testFindAccessibleByUser_WhenUserIsDiretoria_ShouldReturnAllDemands() {
        List<Demand> allDemands = Arrays.asList(testDemand, new Demand());
        when(demandRepository.findAll()).thenReturn(allDemands);

        List<Demand> result = demandService.findAccessibleByUser(testUser, "DIRETORIA");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(demandRepository, times(1)).findAll();
        verify(demandRepository, never()).findAccessibleByUser(any(), any());
    }

    @Test
    public void testFindAccessibleByUser_WhenUserIsRegular_ShouldReturnAccessibleDemands() {
        String userRole = "USER";
        List<Demand> accessibleDemands = Arrays.asList(testDemand);
        when(demandRepository.findAccessibleByUser(testUser, userRole)).thenReturn(accessibleDemands);

        List<Demand> result = demandService.findAccessibleByUser(testUser, userRole);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(demandRepository, times(1)).findAccessibleByUser(testUser, userRole);
        verify(demandRepository, never()).findAll();
    }

    @Test
    public void testDeleteDemand_ShouldCallRepositoryDelete() {
        Long demandId = 1L;

        demandService.deleteDemand(demandId);

        verify(demandRepository, times(1)).deleteById(demandId);
    }

    @Test
    public void testUpdateStatus_WhenDemandExists_ShouldUpdateStatus() {
        when(demandRepository.findById(1L)).thenReturn(Optional.of(testDemand));
        when(demandRepository.save(any(Demand.class))).thenReturn(testDemand);

        Demand result = demandService.updateStatus(1L, DemandStatus.CONCLUIDA);

        assertNotNull(result);
        assertEquals(DemandStatus.CONCLUIDA, result.getStatus());
        verify(demandRepository, times(1)).findById(1L);
        verify(demandRepository, times(1)).save(testDemand);
    }

    @Test
    public void testUpdateStatus_WhenDemandDoesNotExist_ShouldThrowException() {
        when(demandRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            demandService.updateStatus(999L, DemandStatus.CONCLUIDA);
        });

        assertTrue(exception.getMessage().contains("Demanda não encontrada"));
        verify(demandRepository, times(1)).findById(999L);
        verify(demandRepository, never()).save(any());
    }

    @Test
    public void testAssignToUser_WhenDemandExists_ShouldAssignAndUpdateStatus() {
        when(demandRepository.findById(1L)).thenReturn(Optional.of(testDemand));
        when(demandRepository.save(any(Demand.class))).thenReturn(testDemand);

        Demand result = demandService.assignToUser(1L, testUser);

        assertNotNull(result);
        assertEquals(testUser, result.getAssignedTo());
        assertEquals(DemandStatus.EM_ANDAMENTO, result.getStatus());
        verify(demandRepository, times(1)).findById(1L);
        verify(demandRepository, times(1)).save(testDemand);
    }

    @Test
    public void testAssignToUser_WhenDemandDoesNotExist_ShouldThrowException() {
        when(demandRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            demandService.assignToUser(999L, testUser);
        });

        assertTrue(exception.getMessage().contains("Demanda não encontrada"));
        verify(demandRepository, times(1)).findById(999L);
        verify(demandRepository, never()).save(any());
    }
}
