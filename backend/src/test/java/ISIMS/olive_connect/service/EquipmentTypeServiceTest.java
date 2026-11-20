package ISIMS.olive_connect.service;

import ISIMS.olive_connect.dto.CreateEquipmentTypeRequest;
import ISIMS.olive_connect.dto.EquipmentTypeResponse;
import ISIMS.olive_connect.entity.CustomEquipmentType;
import ISIMS.olive_connect.entity.EquipmentType.SpecField;
import ISIMS.olive_connect.repository.CustomEquipmentTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentTypeServiceTest {

    @Mock
    private CustomEquipmentTypeRepository customTypeRepository;

    @InjectMocks
    private EquipmentTypeService service;

    private static CreateEquipmentTypeRequest request(String label) {
        CreateEquipmentTypeRequest request = new CreateEquipmentTypeRequest();
        request.setLabel(label);
        return request;
    }

    @Test
    void toCode_normalisesLabels() {
        assertEquals("TRAILER_BIN", EquipmentTypeService.toCode("  Trailer bin "));
        assertEquals("TRAILER_BIN", EquipmentTypeService.toCode("trailer--bin!"));
        assertEquals("BENNE_À_OLIVES", EquipmentTypeService.toCode("Benne à olives"));
        assertEquals("", EquipmentTypeService.toCode("!!!"));
    }

    @Test
    void getTypes_listsBuiltInsFirstThenCustomTypes() {
        CustomEquipmentType custom = new CustomEquipmentType();
        custom.setCode("TRAILER_BIN");
        custom.setLabel("Trailer bin");
        when(customTypeRepository.findAll()).thenReturn(List.of(custom));

        List<EquipmentTypeResponse> types = service.getTypes();

        assertEquals(List.of("TRACTOR", "BIN", "NET", "VIBRATOR", "TRAILER_BIN"),
                types.stream().map(EquipmentTypeResponse::getCode).toList());
        assertTrue(types.get(1).isBuiltIn());
        assertEquals(List.of(new SpecField("capacityKg", true)), types.get(1).getFields());
        assertFalse(types.get(4).isBuiltIn());
        assertTrue(types.get(4).getFields().isEmpty());
    }

    @Test
    void createType_savesGeneratedCodeAndTrimmedLabel() {
        when(customTypeRepository.save(any(CustomEquipmentType.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipmentTypeResponse response = service.createType(request(" Trailer bin "));

        assertEquals("TRAILER_BIN", response.getCode());
        assertEquals("Trailer bin", response.getLabel());
        assertFalse(response.isBuiltIn());
    }

    @Test
    void createType_matchingBuiltIn_isRejected() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createType(request("bin")));
        assertEquals("Equipment type already exists: BIN", ex.getMessage());
        verify(customTypeRepository, never()).save(any());
    }

    @Test
    void createType_matchingCustomType_isRejected() {
        when(customTypeRepository.existsByCode("TRAILER_BIN")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> service.createType(request("trailer BIN")));
        verify(customTypeRepository, never()).save(any());
    }

    @Test
    void createType_labelWithoutLettersOrDigits_isRejected() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createType(request("!!!")));
        assertEquals("Label must contain letters or digits.", ex.getMessage());
    }

    @Test
    void requireKnownType_acceptsBuiltInAndCustom_rejectsUnknown() {
        when(customTypeRepository.existsByCode("TRAILER_BIN")).thenReturn(true);

        assertDoesNotThrow(() -> service.requireKnownType("BIN"));
        assertDoesNotThrow(() -> service.requireKnownType("TRAILER_BIN"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.requireKnownType("TRUCK"));
        assertEquals("Unknown equipment type: TRUCK", ex.getMessage());
    }
}
