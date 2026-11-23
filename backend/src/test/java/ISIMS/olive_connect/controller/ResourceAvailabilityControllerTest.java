package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.security.AuthEntryPointJwt;
import ISIMS.olive_connect.security.JwtUtils;
import ISIMS.olive_connect.security.SecurityConfig;
import ISIMS.olive_connect.service.CollectionTourService;
import ISIMS.olive_connect.service.EquipmentTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResourceAvailabilityController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class})
class ResourceAvailabilityControllerTest {

    private static final String WORKERS = "/api/collection/resources/workers";
    private static final String EQUIPMENT = "/api/collection/resources/equipment";
    private static final String START = "2026-11-25T08:00:00Z";
    private static final String END = "2026-11-25T17:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionTourService tourService;

    @MockitoBean
    private EquipmentTypeService equipmentTypeService;

    // Required by SecurityConfig
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    private static EquipmentResponse equipment(String id, String type) {
        EquipmentResponse response = new EquipmentResponse();
        response.setId(id);
        response.setType(type);
        return response;
    }

    // --- security ---

    @Test
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get(WORKERS).param("start", START).param("end", END)).andExpect(status().isUnauthorized());
        verifyNoInteractions(tourService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void logisticsManager_isForbidden() throws Exception {
        mockMvc.perform(get(EQUIPMENT).param("start", START).param("end", END)).andExpect(status().isForbidden());
        verifyNoInteractions(tourService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_isForbidden() throws Exception {
        mockMvc.perform(get(WORKERS).param("start", START).param("end", END)).andExpect(status().isForbidden());
        verifyNoInteractions(tourService);
    }

    // --- workers ---

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void workers_returnsAssignableWorkersForThePeriod() throws Exception {
        WorkerResponse worker = new WorkerResponse();
        worker.setId("w1");
        worker.setFullName("Ali");
        when(tourService.getAvailableWorkers(Instant.parse(START), Instant.parse(END), null))
                .thenReturn(List.of(worker));

        mockMvc.perform(get(WORKERS).param("start", START).param("end", END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Ali"));
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void workers_withoutEnd_isBadRequest() throws Exception {
        mockMvc.perform(get(WORKERS).param("start", START)).andExpect(status().isBadRequest());
        verifyNoInteractions(tourService);
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void workers_invalidPeriod_isBadRequest() throws Exception {
        when(tourService.getAvailableWorkers(Instant.parse(END), Instant.parse(START), null))
                .thenThrow(new RuntimeException("Start time must be before end time."));

        mockMvc.perform(get(WORKERS).param("start", END).param("end", START))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start time must be before end time."));
    }

    // --- equipment ---

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void equipment_withoutType_returnsAllAssignable() throws Exception {
        when(tourService.getAvailableEquipment(Instant.parse(START), Instant.parse(END), null))
                .thenReturn(List.of(equipment("e1", "BIN"), equipment("e2", "TRACTOR")));

        mockMvc.perform(get(EQUIPMENT).param("start", START).param("end", END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        verifyNoInteractions(equipmentTypeService);
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void equipment_withType_returnsOnlyThatType() throws Exception {
        when(tourService.getAvailableEquipment(Instant.parse(START), Instant.parse(END), null))
                .thenReturn(List.of(equipment("e1", "BIN"), equipment("e2", "TRACTOR")));

        mockMvc.perform(get(EQUIPMENT).param("start", START).param("end", END).param("type", "BIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("e1"));
        verify(equipmentTypeService).requireKnownType("BIN");
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void equipment_withUnknownType_isBadRequest() throws Exception {
        doThrow(new RuntimeException("Unknown equipment type: TRUCK"))
                .when(equipmentTypeService).requireKnownType("TRUCK");

        mockMvc.perform(get(EQUIPMENT).param("start", START).param("end", END).param("type", "TRUCK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown equipment type: TRUCK"));
        verifyNoInteractions(tourService);
    }
}
