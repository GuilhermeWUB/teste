package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.EventBoardSnapshot;
import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.repository.EventRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    private Event testEvent;
    private Partner testPartner;
    private Vehicle testVehicle;

    @BeforeEach
    public void setUp() {
        testPartner = new Partner();
        testPartner.setId(1L);
        testPartner.setName("Test Partner");

        testVehicle = new Vehicle();
        testVehicle.setId(1L);
        testVehicle.setPlaque("ABC1234");

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setTitulo("Test Event");
        testEvent.setDescricao("Test Description");
        testEvent.setStatus(Status.COMUNICADO);
        testEvent.setPartner(testPartner);
        testEvent.setVehicle(testVehicle);
        testEvent.setDataVencimento(LocalDate.now());
    }

    @Test
    public void testListAll_ShouldReturnAllEvents() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findAll()).thenReturn(events);

        List<Event> result = eventService.listAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    public void testListAllWithRelations_ShouldReturnOrderedEvents() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findAllByOrderByStatusAscDataVencimentoAscIdAsc()).thenReturn(events);

        List<Event> result = eventService.listAllWithRelations();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findAllByOrderByStatusAscDataVencimentoAscIdAsc();
    }

    @Test
    public void testListByStatus_ShouldReturnEventsWithGivenStatus() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findByStatus(Status.COMUNICADO)).thenReturn(events);

        List<Event> result = eventService.listByStatus(Status.COMUNICADO);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Status.COMUNICADO, result.get(0).getStatus());
        verify(eventRepository, times(1)).findByStatus(Status.COMUNICADO);
    }

    @Test
    public void testListByPartnerId_ShouldReturnPartnerEvents() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findByPartnerId(1L)).thenReturn(events);

        List<Event> result = eventService.listByPartnerId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findByPartnerId(1L);
    }

    @Test
    public void testFindById_WhenEventExists_ShouldReturnEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        Optional<Event> result = eventService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(testEvent.getId(), result.get().getId());
        verify(eventRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_WhenEventDoesNotExist_ShouldReturnEmpty() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Event> result = eventService.findById(999L);

        assertFalse(result.isPresent());
        verify(eventRepository, times(1)).findById(999L);
    }

    @Test
    public void testGetBoardSnapshot_ShouldReturnGroupedEvents() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findAllByOrderByStatusAscDataVencimentoAscIdAsc()).thenReturn(events);

        EventBoardSnapshot result = eventService.getBoardSnapshot();

        assertNotNull(result);
        assertNotNull(result.cards());
        assertNotNull(result.grouped());
        assertNotNull(result.counters());
        assertEquals(1, result.cards().size());
        verify(eventRepository, times(1)).findAllByOrderByStatusAscDataVencimentoAscIdAsc();
    }

    @Test
    public void testCreate_WithValidData_ShouldCreateEvent() {
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.create(testEvent);

        assertNotNull(result);
        assertEquals("Test Event", result.getTitulo());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    public void testCreate_WithNullStatus_ShouldSetDefaultStatus() {
        testEvent.setStatus(null);
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            assertEquals(Status.COMUNICADO, event.getStatus());
            return event;
        });

        eventService.create(testEvent);

        verify(eventRepository, times(1)).save(argThat(event -> event.getStatus() == Status.COMUNICADO));
    }

    @Test
    public void testCreate_WhenPartnerNotFound_ShouldThrowException() {
        when(partnerRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            eventService.create(testEvent);
        });

        assertTrue(exception.getMessage().contains("Partner não encontrado"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    public void testCreate_WhenVehicleNotFound_ShouldThrowException() {
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            eventService.create(testEvent);
        });

        assertTrue(exception.getMessage().contains("Vehicle não encontrado"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    public void testCreate_WithVehiclePlaque_ShouldFindVehicleByPartnerAndPlaque() {
        Event newEvent = new Event();
        newEvent.setTitulo("New Event");
        Partner partner = new Partner();
        partner.setId(1L);
        newEvent.setPartner(partner);
        Vehicle vehicle = new Vehicle();
        vehicle.setPlaque("XYZ5678");
        newEvent.setVehicle(vehicle);

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(vehicleRepository.findByPartnerIdAndPlaque(1L, "XYZ5678")).thenReturn(Optional.of(testVehicle));
        when(eventRepository.save(any(Event.class))).thenReturn(newEvent);

        Event result = eventService.create(newEvent);

        assertNotNull(result);
        verify(vehicleRepository, times(1)).findByPartnerIdAndPlaque(1L, "XYZ5678");
    }

    @Test
    public void testUpdate_ShouldUpdateAllFields() {
        Event updatedEvent = new Event();
        updatedEvent.setTitulo("Updated Title");
        updatedEvent.setDescricao("Updated Description");
        updatedEvent.setStatus(Status.ENTREGUES);
        updatedEvent.setPrioridade(Prioridade.ALTA);
        updatedEvent.setPartner(testPartner);
        updatedEvent.setVehicle(testVehicle);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.update(1L, updatedEvent);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitulo());
        assertEquals(Status.ENTREGUES, result.getStatus());
        verify(eventRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).save(testEvent);
    }

    @Test
    public void testUpdate_WhenEventNotFound_ShouldThrowException() {
        Event updatedEvent = new Event();
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.update(999L, updatedEvent);
        });

        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    public void testUpdateStatus_ShouldUpdateOnlyStatus() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.updateStatus(1L, Status.VISTORIA);

        assertNotNull(result);
        assertEquals(Status.VISTORIA, result.getStatus());
        verify(eventRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).save(testEvent);
    }

    @Test
    public void testUpdateStatus_WhenEventNotFound_ShouldThrowException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.updateStatus(999L, Status.ENTREGUES);
        });

        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    public void testUpdatePartial_ShouldUpdateSpecifiedFields() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("titulo", "Partially Updated Title");
        updates.put("status", "VISTORIA");
        updates.put("prioridade", "ALTA");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.updatePartial(1L, updates);

        assertNotNull(result);
        assertEquals("Partially Updated Title", result.getTitulo());
        assertEquals(Status.VISTORIA, result.getStatus());
        assertEquals(Prioridade.ALTA, result.getPrioridade());
        verify(eventRepository, times(1)).save(testEvent);
    }

    @Test
    public void testUpdatePartial_WhenEventNotFound_ShouldThrowException() {
        Map<String, Object> updates = new HashMap<>();
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.updatePartial(999L, updates);
        });

        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    public void testDelete_WhenEventExists_ShouldDeleteEvent() {
        when(eventRepository.existsById(1L)).thenReturn(true);

        eventService.delete(1L);

        verify(eventRepository, times(1)).existsById(1L);
        verify(eventRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testDelete_WhenEventDoesNotExist_ShouldNotThrowException() {
        when(eventRepository.existsById(999L)).thenReturn(false);

        assertDoesNotThrow(() -> eventService.delete(999L));

        verify(eventRepository, times(1)).existsById(999L);
        verify(eventRepository, never()).deleteById(any());
    }
}
