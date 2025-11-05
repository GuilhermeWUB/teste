package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Address;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.PartnerStatus;
import com.necsus.necsusspring.repository.AddressRepository;
import com.necsus.necsusspring.repository.AdhesionRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PartnerServiceTest {

    @InjectMocks
    private PartnerService partnerService;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AdhesionRepository adhesionRepository;

    /**
     * This test demonstrates the bug: attempting to update a partner that has no address
     * with new address information causes a NullPointerException.
     *
     * Bug location: PartnerService.updatePartner() lines 73-81
     * The code checks if partner.getAddress() != null but then calls
     * existingPartner.getAddress().setXxx() without checking if existingPartner.getAddress() is null.
     */
    @Test
    public void testUpdatePartner_WhenExistingPartnerHasNoAddress_ShouldNotThrowNPE() {
        // Arrange: Create a partner WITHOUT an address (simulating a partner created without address data)
        Long partnerId = 1L;
        Partner existingPartner = new Partner();
        existingPartner.setId(partnerId);
        existingPartner.setName("John Doe");
        existingPartner.setEmail("john@example.com");
        existingPartner.setCpf("12345678900");
        existingPartner.setStatus(PartnerStatus.ATIVO);
        existingPartner.setDateBorn(LocalDate.of(1990, 1, 1));
        existingPartner.setAddress(null); // No address initially

        // Create an update payload WITH an address
        Partner updatePayload = new Partner();
        updatePayload.setId(partnerId);
        updatePayload.setName("John Doe Updated");
        updatePayload.setEmail("john.updated@example.com");
        updatePayload.setCpf("12345678900");
        updatePayload.setStatus(PartnerStatus.ATIVO);
        updatePayload.setDateBorn(LocalDate.of(1990, 1, 1));

        // Create address to add
        Address newAddress = new Address();
        newAddress.setAddress("123 Main Street");
        newAddress.setNeighborhood("Downtown");
        newAddress.setCity("São Paulo");
        newAddress.setZipcode("01234-567");
        newAddress.setNumber("123");
        newAddress.setStates("SP");
        updatePayload.setAddress(newAddress);

        // Mock repository behavior
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(existingPartner));
        when(partnerRepository.save(any(Partner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert: This should NOT throw NullPointerException
        assertDoesNotThrow(() -> {
            Partner result = partnerService.updatePartner(updatePayload);

            // Verify the partner was updated
            assertNotNull(result);
            assertEquals("John Doe Updated", result.getName());
            assertEquals("john.updated@example.com", result.getEmail());

            // Verify the address was properly set (not null)
            assertNotNull(result.getAddress(), "Address should have been created/set");
            assertEquals("123 Main Street", result.getAddress().getAddress());
            assertEquals("Downtown", result.getAddress().getNeighborhood());
            assertEquals("São Paulo", result.getAddress().getCity());
            assertEquals("01234-567", result.getAddress().getZipcode());
            assertEquals("123", result.getAddress().getNumber());
            assertEquals("SP", result.getAddress().getStates());
        });

        // Verify save was called
        verify(partnerRepository, times(1)).save(any(Partner.class));
    }

    /**
     * Additional test: Verify that updating a partner that already has an address works correctly
     */
    @Test
    public void testUpdatePartner_WhenExistingPartnerHasAddress_ShouldUpdateAddressFields() {
        // Arrange: Create a partner WITH an existing address
        Long partnerId = 1L;
        Partner existingPartner = new Partner();
        existingPartner.setId(partnerId);
        existingPartner.setName("Jane Doe");
        existingPartner.setEmail("jane@example.com");
        existingPartner.setCpf("98765432100");
        existingPartner.setStatus(PartnerStatus.ATIVO);

        // Existing address
        Address existingAddress = new Address();
        existingAddress.setId(100L);
        existingAddress.setAddress("Old Street");
        existingAddress.setNeighborhood("Old Neighborhood");
        existingAddress.setCity("Rio de Janeiro");
        existingAddress.setZipcode("99999-999");
        existingAddress.setNumber("999");
        existingAddress.setStates("RJ");
        existingPartner.setAddress(existingAddress);

        // Create update payload with new address
        Partner updatePayload = new Partner();
        updatePayload.setId(partnerId);
        updatePayload.setName("Jane Doe Updated");
        updatePayload.setEmail("jane@example.com");
        updatePayload.setCpf("98765432100");

        Address newAddress = new Address();
        newAddress.setAddress("New Street");
        newAddress.setNeighborhood("New Neighborhood");
        newAddress.setCity("Brasília");
        newAddress.setZipcode("12345-678");
        newAddress.setNumber("456");
        newAddress.setStates("DF");
        updatePayload.setAddress(newAddress);

        // Mock repository behavior
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(existingPartner));
        when(partnerRepository.save(any(Partner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Partner result = partnerService.updatePartner(updatePayload);

        // Assert: Verify the address fields were updated
        assertNotNull(result);
        assertNotNull(result.getAddress());
        assertEquals("New Street", result.getAddress().getAddress());
        assertEquals("New Neighborhood", result.getAddress().getNeighborhood());
        assertEquals("Brasília", result.getAddress().getCity());
        assertEquals("12345-678", result.getAddress().getZipcode());
        assertEquals("456", result.getAddress().getNumber());
        assertEquals("DF", result.getAddress().getStates());

        // Verify the same address object was updated (not replaced)
        assertEquals(100L, result.getAddress().getId());
    }
}
