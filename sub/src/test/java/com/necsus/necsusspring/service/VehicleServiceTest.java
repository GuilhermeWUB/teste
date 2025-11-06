package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Payment;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.repository.EventRepository;
import com.necsus.necsusspring.repository.PaymentRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VehicleServiceTest {

    @InjectMocks
    private VehicleService vehicleService;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EventRepository eventRepository;

    private Vehicle testVehicle;
    private Partner testPartner;
    private Payment testPayment;

    @BeforeEach
    public void setUp() {
        testPartner = new Partner();
        testPartner.setId(1L);
        testPartner.setName("Test Partner");
        testPartner.setCpf("12345678900");

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setMonthly(new BigDecimal("100.00"));
        testPayment.setVencimento(10);

        testVehicle = new Vehicle();
        testVehicle.setId(1L);
        testVehicle.setPlaque("ABC1234");
        testVehicle.setMaker("Toyota");
        testVehicle.setModel("Corolla");
        testVehicle.setPartnerId(1L);
        testVehicle.setPartner(testPartner);
        testVehicle.setPayment(testPayment);
    }

    @Test
    public void testListAll_WithoutPartnerId_ShouldReturnAllVehicles() {
        List<Vehicle> vehicles = Arrays.asList(testVehicle);
        when(vehicleRepository.findAll()).thenReturn(vehicles);
        when(paymentRepository.findByVehicleId(1L)).thenReturn(Optional.of(testPayment));

        List<Vehicle> result = vehicleService.listAll(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(vehicleRepository, times(1)).findAll();
        verify(vehicleRepository, never()).findByPartnerId(any());
    }

    @Test
    public void testListAll_WithPartnerId_ShouldReturnPartnerVehicles() {
        List<Vehicle> vehicles = Arrays.asList(testVehicle);
        when(vehicleRepository.findByPartnerId(1L)).thenReturn(vehicles);
        when(paymentRepository.findByVehicleId(1L)).thenReturn(Optional.of(testPayment));

        List<Vehicle> result = vehicleService.listAll(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(vehicleRepository, times(1)).findByPartnerId(1L);
        verify(vehicleRepository, never()).findAll();
    }

    @Test
    public void testListAllPaginated_WithoutPartnerId_ShouldReturnPagedVehicles() {
        Page<Vehicle> vehiclePage = new PageImpl<>(Arrays.asList(testVehicle));
        when(vehicleRepository.findAll(any(Pageable.class))).thenReturn(vehiclePage);
        when(paymentRepository.findByVehicleId(1L)).thenReturn(Optional.of(testPayment));

        Page<Vehicle> result = vehicleService.listAllPaginated(null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(vehicleRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    public void testListAllPaginated_WithPartnerId_ShouldReturnPagedPartnerVehicles() {
        Page<Vehicle> vehiclePage = new PageImpl<>(Arrays.asList(testVehicle));
        when(vehicleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(vehiclePage);
        when(paymentRepository.findByVehicleId(1L)).thenReturn(Optional.of(testPayment));

        Page<Vehicle> result = vehicleService.listAllPaginated(1L, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(vehicleRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    public void testListAllPaginated_ShouldLimitPageSizeTo30() {
        Page<Vehicle> vehiclePage = new PageImpl<>(Arrays.asList(testVehicle));
        when(vehicleRepository.findAll(any(Pageable.class))).thenReturn(vehiclePage);
        when(paymentRepository.findByVehicleId(1L)).thenReturn(Optional.of(testPayment));

        vehicleService.listAllPaginated(null, 0, 100);

        verify(vehicleRepository, times(1)).findAll(argThat((Pageable p) -> p.getPageSize() == 30));
    }

    @Test
    public void testListByPartnerId_WithNullPartnerId_ShouldReturnEmptyList() {
        List<Vehicle> result = vehicleService.listByPartnerId(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(vehicleRepository, never()).findByPartnerId(any());
    }

    @Test
    public void testListByPartnerId_WithValidPartnerId_ShouldReturnVehicles() {
        List<Vehicle> vehicles = Arrays.asList(testVehicle);
        when(vehicleRepository.findByPartnerId(1L)).thenReturn(vehicles);
        when(paymentRepository.findByVehicleId(1L)).thenReturn(Optional.of(testPayment));

        List<Vehicle> result = vehicleService.listByPartnerId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(vehicleRepository, times(1)).findByPartnerId(1L);
    }

    @Test
    public void testFindById_WhenVehicleExists_ShouldReturnVehicle() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(paymentRepository.findByVehicleId(1L)).thenReturn(Optional.of(testPayment));

        Optional<Vehicle> result = vehicleService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(testVehicle.getId(), result.get().getId());
        verify(vehicleRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_WhenVehicleDoesNotExist_ShouldReturnEmpty() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Vehicle> result = vehicleService.findById(999L);

        assertFalse(result.isPresent());
        verify(vehicleRepository, times(1)).findById(999L);
    }

    @Test
    public void testCreate_ShouldSaveVehicleAndPayment() {
        Vehicle newVehicle = new Vehicle();
        newVehicle.setPlaque("XYZ9876");
        newVehicle.setPartnerId(1L);
        newVehicle.setPayment(testPayment);

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(testVehicle);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Vehicle result = vehicleService.create(newVehicle);

        assertNotNull(result);
        verify(partnerRepository, times(1)).findById(1L);
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    public void testCreate_WhenPartnerNotFound_ShouldThrowException() {
        Vehicle newVehicle = new Vehicle();
        newVehicle.setPartnerId(999L);

        when(partnerRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            vehicleService.create(newVehicle);
        });

        assertTrue(exception.getMessage().contains("Partner not found"));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    public void testUpdate_ShouldUpdateVehicleAndPayment() {
        Vehicle updatedVehicle = new Vehicle();
        updatedVehicle.setId(1L);
        updatedVehicle.setPlaque("NEW1234");
        updatedVehicle.setMaker("Honda");
        updatedVehicle.setModel("Civic");
        updatedVehicle.setPartnerId(1L);
        updatedVehicle.setPayment(testPayment);

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(testVehicle);
        when(paymentRepository.findByVehicleId(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Vehicle result = vehicleService.update(1L, updatedVehicle);

        assertNotNull(result);
        verify(vehicleRepository, times(1)).findById(1L);
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    public void testUpdate_WhenVehicleNotFound_ShouldThrowException() {
        Vehicle updatedVehicle = new Vehicle();
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            vehicleService.update(999L, updatedVehicle);
        });

        assertTrue(exception.getMessage().contains("Vehicle not found"));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    public void testDelete_WhenVehicleExists_ShouldDeleteVehicleAndRelatedData() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);

        vehicleService.delete(1L);

        verify(eventRepository, times(1)).deleteByVehicleId(1L);
        verify(paymentRepository, times(1)).deleteByVehicleId(1L);
        verify(vehicleRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testDelete_WhenVehicleDoesNotExist_ShouldNotDeleteAnything() {
        when(vehicleRepository.existsById(999L)).thenReturn(false);

        vehicleService.delete(999L);

        verify(vehicleRepository, never()).deleteById(any());
        verify(eventRepository, never()).deleteByVehicleId(any());
        verify(paymentRepository, never()).deleteByVehicleId(any());
    }
}
