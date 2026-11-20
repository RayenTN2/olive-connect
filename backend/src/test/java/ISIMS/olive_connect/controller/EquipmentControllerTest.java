package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.EquipmentResponse;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.security.AuthEntryPointJwt;
import ISIMS.olive_connect.security.JwtUtils;
import ISIMS.olive_connect.security.SecurityConfig;
import ISIMS.olive_connect.service.EquipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EquipmentController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class})
class EquipmentControllerTest {

    private static final String URL = "/api/logistics/equipment";
    private static final String VALID_BODY = "{\"name\":\"Tractor A\",\"reference\":\"TR-01\",\"type\":\"TRACTOR\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipmentService equipmentService;

    // Required by SecurityConfig
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    private EquipmentResponse sampleResponse() {
        EquipmentResponse response = new EquipmentResponse();
        response.setId("e1");
        response.setName("Tractor A");
        response.setReference("TR-01");
        response.setType("TRACTOR");
        response.setActive(true);
        return response;
    }

    // --- security ---

    @Test
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(equipmentService);
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void collectionManager_isForbidden() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
        verifyNoInteractions(equipmentService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_isForbidden() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(equipmentService);
    }

    // --- create ---

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_returns201() throws Exception {
        when(equipmentService.createEquipment(any())).thenReturn(sampleResponse());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("e1"))
                .andExpect(jsonPath("$.type").value("TRACTOR"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withoutReference_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tractor A\",\"type\":\"TRACTOR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("reference: Reference is required"));
        verifyNoInteractions(equipmentService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withoutType_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tractor A\",\"reference\":\"TR-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("type: Type is required"));
        verifyNoInteractions(equipmentService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withUnknownType_isBadRequest() throws Exception {
        when(equipmentService.createEquipment(any())).thenThrow(new RuntimeException("Unknown equipment type: TRUCK"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Truck\",\"reference\":\"TK-01\",\"type\":\"TRUCK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown equipment type: TRUCK"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withNonPositiveCapacity_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bin\",\"reference\":\"BN-01\",\"type\":\"BIN\",\"capacityKg\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("capacityKg: Capacity must be a positive number of kg"));
        verifyNoInteractions(equipmentService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withDuplicateReference_isBadRequest() throws Exception {
        when(equipmentService.createEquipment(any()))
                .thenThrow(new RuntimeException("Reference is already in use: TR-01"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reference is already in use: TR-01"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_rejectedByUniqueIndex_isBadRequestWithoutMongoDetails() throws Exception {
        when(equipmentService.createEquipment(any()))
                .thenThrow(new DuplicateKeyException("E11000 duplicate key error collection: oliveconnect.equipment"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This value is already in use."));
    }

    // --- list / consult ---

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void list_passesFilters() throws Exception {
        when(equipmentService.getEquipment("BIN", true)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get(URL).param("type", "BIN").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void list_withUnknownType_isBadRequest() throws Exception {
        when(equipmentService.getEquipment("TRUCK", null)).thenThrow(new RuntimeException("Unknown equipment type: TRUCK"));

        mockMvc.perform(get(URL).param("type", "TRUCK")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void getById_returns200() throws Exception {
        when(equipmentService.getEquipmentById("e1")).thenReturn(sampleResponse());

        mockMvc.perform(get(URL + "/e1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("TR-01"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void getById_unknown_isNotFound() throws Exception {
        when(equipmentService.getEquipmentById("e9"))
                .thenThrow(new ResourceNotFoundException("Equipment not found with id: e9"));

        mockMvc.perform(get(URL + "/e9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Equipment not found with id: e9"));
    }

    // --- update / status ---

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void update_returns200() throws Exception {
        when(equipmentService.updateEquipment(eq("e1"), any())).thenReturn(sampleResponse());

        mockMvc.perform(put(URL + "/e1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("e1"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void updateStatus_returns200() throws Exception {
        EquipmentResponse inactive = sampleResponse();
        inactive.setActive(false);
        when(equipmentService.updateEquipmentStatus("e1", false)).thenReturn(inactive);

        mockMvc.perform(patch(URL + "/e1/status").contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void updateStatus_withoutActive_isBadRequest() throws Exception {
        mockMvc.perform(patch(URL + "/e1/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("active: Active status is required"));
        verifyNoInteractions(equipmentService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void updateStatus_unknown_isNotFound() throws Exception {
        when(equipmentService.updateEquipmentStatus("e9", true))
                .thenThrow(new ResourceNotFoundException("Equipment not found with id: e9"));

        mockMvc.perform(patch(URL + "/e9/status").contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isNotFound());
    }
}
