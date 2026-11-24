package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.EquipmentStatusResponse;
import ISIMS.olive_connect.dto.EquipmentUnavailabilityResponse;
import ISIMS.olive_connect.entity.EquipmentStatus;
import ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.security.AuthEntryPointJwt;
import ISIMS.olive_connect.security.JwtUtils;
import ISIMS.olive_connect.security.SecurityConfig;
import ISIMS.olive_connect.service.CollectionTourService;
import ISIMS.olive_connect.service.EquipmentAvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EquipmentUnavailabilityController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class})
class EquipmentUnavailabilityControllerTest {

    private static final String URL = "/api/logistics/equipment/e1/unavailabilities";
    private static final String VALID_BODY =
            "{\"startTime\":\"2026-11-25T10:00:00Z\",\"endTime\":\"2026-11-25T12:00:00Z\","
                    + "\"status\":\"UNDER_MAINTENANCE\",\"reason\":\"Oil change\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipmentAvailabilityService availabilityService;

    @MockitoBean
    private CollectionTourService tourService;

    // Required by SecurityConfig
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    private EquipmentUnavailabilityResponse sampleResponse() {
        EquipmentUnavailabilityResponse response = new EquipmentUnavailabilityResponse();
        response.setId("u1");
        response.setEquipmentId("e1");
        response.setStartTime(Instant.parse("2026-11-25T10:00:00Z"));
        response.setEndTime(Instant.parse("2026-11-25T12:00:00Z"));
        response.setStatus(EquipmentUnavailabilityStatus.UNDER_MAINTENANCE);
        response.setReason("Oil change");
        return response;
    }

    // --- security ---

    @Test
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(availabilityService);
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void collectionManager_isForbidden() throws Exception {
        mockMvc.perform(get("/api/logistics/equipment/e1/availability")).andExpect(status().isForbidden());
        verifyNoInteractions(availabilityService, tourService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_isAllowed() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(403, result.getResponse().getStatus()));
    }

    // --- create ---

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_returns201() throws Exception {
        when(availabilityService.addUnavailability(eq("e1"), any())).thenReturn(sampleResponse());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("u1"))
                .andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withoutStatus_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"2026-11-25T10:00:00Z\",\"endTime\":\"2026-11-25T12:00:00Z\",\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status: Status is required"));
        verifyNoInteractions(availabilityService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withInvalidStatus_isBadRequest() throws Exception {
        // IN_USE / AVAILABLE are computed, never recorded by hand
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("UNDER_MAINTENANCE", "IN_USE")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(availabilityService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withoutReason_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace(",\"reason\":\"Oil change\"", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("reason: Reason is required"));
        verifyNoInteractions(availabilityService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withStartAfterEnd_isBadRequest() throws Exception {
        when(availabilityService.addUnavailability(eq("e1"), any()))
                .thenThrow(new RuntimeException("Start time must be before end time."));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start time must be before end time."));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_forUnknownEquipment_isNotFound() throws Exception {
        when(availabilityService.addUnavailability(eq("e1"), any()))
                .thenThrow(new ResourceNotFoundException("Equipment not found with id: e1"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Equipment not found with id: e1"));
    }

    // --- list / edit / delete ---

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void list_returnsPeriods() throws Exception {
        when(availabilityService.getUnavailabilities("e1")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void update_returns200() throws Exception {
        when(availabilityService.updateUnavailability(eq("e1"), eq("u1"), any())).thenReturn(sampleResponse());

        mockMvc.perform(put(URL + "/u1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u1"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void delete_returns204WithoutBody() throws Exception {
        mockMvc.perform(delete(URL + "/u1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(availabilityService).deleteUnavailability("e1", "u1");
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void delete_unknownPeriod_isNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Unavailability not found with id: u9 for equipment: e1"))
                .when(availabilityService).deleteUnavailability("e1", "u9");

        mockMvc.perform(delete(URL + "/u9")).andExpect(status().isNotFound());
    }

    // --- status ---

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void status_returnsCurrentStatus() throws Exception {
        when(tourService.getEquipmentStatus("e1"))
                .thenReturn(new EquipmentStatusResponse("e1", EquipmentStatus.UNDER_MAINTENANCE, true));

        mockMvc.perform(get("/api/logistics/equipment/e1/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipmentId").value("e1"))
                .andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void status_unknownEquipment_isNotFound() throws Exception {
        when(tourService.getEquipmentStatus("e9"))
                .thenThrow(new ResourceNotFoundException("Equipment not found with id: e9"));

        mockMvc.perform(get("/api/logistics/equipment/e9/availability")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void status_canBeInUse() throws Exception {
        when(tourService.getEquipmentStatus("e1"))
                .thenReturn(new EquipmentStatusResponse("e1", EquipmentStatus.IN_USE, true));

        mockMvc.perform(get("/api/logistics/equipment/e1/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_USE"));
    }
}
