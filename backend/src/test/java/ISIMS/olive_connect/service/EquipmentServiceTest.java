package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.EquipmentRequest;
import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.entity.Equipment;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private EquipmentTypeService equipmentTypeService;

    @InjectMocks
    private EquipmentService service;

    @BeforeEach
    void setUp() {
        lenient().when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static EquipmentRequest request(String name, String reference, String type) {
        EquipmentRequest request = new EquipmentRequest();
        request.setName(name);
        request.setReference(reference);
        request.setType(type);
        return request;
    }

    private static EquipmentRequest bin(String reference, Integer capacityKg) {
        EquipmentRequest request = request("Bin", reference, "BIN");
        request.setCapacityKg(capacityKg);
        return request;
    }

    private static Equipment equipment(String id, boolean active) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setName("Tractor A");
        equipment.setReference("TR-01");
        equipment.setType("TRACTOR");
        equipment.setActive(active);
        equipment.setCreatedAt(CREATED);
        equipment.setUpdatedAt(CREATED);
        return equipment;
    }

    // --- create ---

    @Test
    void createEquipment_savesActiveEquipmentWithSpecs() {
        EquipmentRequest request = request("Tractor A", "TR-01", "TRACTOR");
        request.setBrand("New Holland");
        request.setModel("TD5");
        request.setSerialNumber("NH123");
        request.setRegistrationNumber("123 TU 4567");

        EquipmentResponse response = service.createEquipment(request);

        verify(equipmentTypeService).requireKnownType("TRACTOR");
        assertEquals("TRACTOR", response.getType());
        assertEquals("New Holland", response.getBrand());
        assertEquals("TD5", response.getModel());
        assertEquals("NH123", response.getSerialNumber());
        assertEquals("123 TU 4567", response.getRegistrationNumber());
        assertNull(response.getCapacityKg());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void createEquipment_binWithCapacity_isSaved() {
        assertEquals(500, service.createEquipment(bin("BN-01", 500)).getCapacityKg());
    }

    @Test
    void createEquipment_customType_isSaved() {
        assertEquals("TRAILER_BIN", service.createEquipment(request("Trailer", "TB-01", "TRAILER_BIN")).getType());
        verify(equipmentTypeService).requireKnownType("TRAILER_BIN");
    }

    @Test
    void createEquipment_unknownType_isRejected() {
        doThrow(new RuntimeException("Unknown equipment type: TRUCK")).when(equipmentTypeService).requireKnownType("TRUCK");

        assertThrows(RuntimeException.class, () -> service.createEquipment(request("Truck", "TK-01", "TRUCK")));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void createEquipment_binWithoutCapacity_isRejected() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createEquipment(bin("BN-01", null)));
        assertEquals("Capacity is required for bins.", ex.getMessage());
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void createEquipment_capacityOnNonBin_isRejected() {
        EquipmentRequest request = request("Net", "NT-01", "NET");
        request.setCapacityKg(100);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createEquipment(request));
        assertEquals("Capacity is only allowed for bins.", ex.getMessage());
    }

    @Test
    void createEquipment_registrationOnNonTractor_isRejected() {
        EquipmentRequest request = bin("BN-01", 500);
        request.setRegistrationNumber("123 TU 4567");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createEquipment(request));
        assertEquals("Registration number is only allowed for tractors.", ex.getMessage());
    }

    @Test
    void createEquipment_blankOptionalFields_areTreatedAsMissing() {
        EquipmentRequest request = bin("BN-02", 500);
        request.setRegistrationNumber("");   // what an empty form field sends
        request.setBrand("  ");
        request.setModel("");
        request.setSerialNumber("");

        EquipmentResponse response = service.createEquipment(request);

        assertNull(response.getRegistrationNumber());
        assertNull(response.getBrand());
        assertNull(response.getModel());
        assertNull(response.getSerialNumber());
    }

    @Test
    void createEquipment_duplicateReference_isRejected() {
        when(equipmentRepository.existsByReference("TR-01")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createEquipment(request("Tractor B", "TR-01", "TRACTOR")));
        assertEquals("Reference is already in use: TR-01", ex.getMessage());
        verify(equipmentRepository, never()).save(any());
    }

    // --- list ---

    @Test
    void getEquipment_withoutFilters_returnsAll() {
        when(equipmentRepository.findAll()).thenReturn(List.of(equipment("e1", true), equipment("e2", false)));
        assertEquals(2, service.getEquipment(null, null).size());
        verifyNoInteractions(equipmentTypeService);
    }

    @Test
    void getEquipment_byType() {
        when(equipmentRepository.findByType("NET")).thenReturn(List.of(equipment("e1", true)));
        assertEquals(1, service.getEquipment("NET", null).size());
    }

    @Test
    void getEquipment_byActive() {
        when(equipmentRepository.findByActive(false)).thenReturn(List.of(equipment("e2", false)));
        assertEquals(1, service.getEquipment(null, false).size());
    }

    @Test
    void getEquipment_byTypeAndActive() {
        when(equipmentRepository.findByTypeAndActive("VIBRATOR", true)).thenReturn(List.of());
        assertTrue(service.getEquipment("VIBRATOR", true).isEmpty());
        verify(equipmentRepository, never()).findAll();
    }

    @Test
    void getEquipment_unknownType_isRejected() {
        doThrow(new RuntimeException("Unknown equipment type: TRUCK")).when(equipmentTypeService).requireKnownType("TRUCK");

        assertThrows(RuntimeException.class, () -> service.getEquipment("TRUCK", null));
        verifyNoInteractions(equipmentRepository);
    }

    // --- consult ---

    @Test
    void getEquipmentById_unknownId_isNotFound() {
        when(equipmentRepository.findById("missing")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getEquipmentById("missing"));
        assertEquals("Equipment not found with id: missing", ex.getMessage());
    }

    // --- update ---

    @Test
    void updateEquipment_replacesDetailsAndKeepsCreatedAtAndStatus() {
        Equipment existing = equipment("e1", false);
        existing.setRegistrationNumber("123 TU 4567");
        when(equipmentRepository.findById("e1")).thenReturn(Optional.of(existing));

        EquipmentResponse response = service.updateEquipment("e1", bin("BN-07", 800));

        assertEquals("BN-07", response.getReference());
        assertEquals("BIN", response.getType());
        assertEquals(800, response.getCapacityKg());
        assertNull(response.getRegistrationNumber()); // PUT replaces all fields
        assertFalse(response.isActive());
        assertEquals(CREATED, response.getCreatedAt());
        assertTrue(response.getUpdatedAt().isAfter(CREATED));
    }

    @Test
    void updateEquipment_keepingItsOwnReference_isAllowed() {
        when(equipmentRepository.findById("e1")).thenReturn(Optional.of(equipment("e1", true)));
        when(equipmentRepository.existsByReferenceAndIdNot("TR-01", "e1")).thenReturn(false);

        assertEquals("TR-01", service.updateEquipment("e1", request("Renamed", "TR-01", "TRACTOR")).getReference());
    }

    @Test
    void updateEquipment_referenceOfAnotherItem_isRejected() {
        when(equipmentRepository.findById("e1")).thenReturn(Optional.of(equipment("e1", true)));
        when(equipmentRepository.existsByReferenceAndIdNot("BN-01", "e1")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> service.updateEquipment("e1", request("Tractor A", "BN-01", "TRACTOR")));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void updateEquipment_binWithoutCapacity_isRejected() {
        when(equipmentRepository.findById("e1")).thenReturn(Optional.of(equipment("e1", true)));

        assertThrows(RuntimeException.class, () -> service.updateEquipment("e1", bin("BN-01", null)));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void updateEquipment_unknownId_isNotFound() {
        when(equipmentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateEquipment("missing", request("x", "y", "NET")));
        verify(equipmentRepository, never()).save(any());
    }

    // --- status ---

    @Test
    void updateEquipmentStatus_deactivatesWithoutDeleting() {
        when(equipmentRepository.findById("e1")).thenReturn(Optional.of(equipment("e1", true)));

        assertFalse(service.updateEquipmentStatus("e1", false).isActive());
        verify(equipmentRepository, never()).delete(any());
        verify(equipmentRepository, never()).deleteById(any());
    }

    @Test
    void updateEquipmentStatus_unknownId_isNotFound() {
        when(equipmentRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateEquipmentStatus("missing", true));
    }
}
