package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.dto.WorkerUnavailabilityResponse;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.security.AuthEntryPointJwt;
import ISIMS.olive_connect.security.JwtUtils;
import ISIMS.olive_connect.security.SecurityConfig;
import ISIMS.olive_connect.service.WorkerAvailabilityService;
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

@WebMvcTest(WorkerUnavailabilityController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class})
class WorkerUnavailabilityControllerTest {

    private static final String URL = "/api/logistics/workers/w1/unavailabilities";
    private static final String VALID_BODY =
            "{\"startTime\":\"2026-11-20T10:00:00Z\",\"endTime\":\"2026-11-20T12:00:00Z\",\"reason\":\"Congé\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkerAvailabilityService availabilityService;

    // Required by SecurityConfig
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    private WorkerUnavailabilityResponse sampleResponse() {
        WorkerUnavailabilityResponse response = new WorkerUnavailabilityResponse();
        response.setId("u1");
        response.setWorkerId("w1");
        response.setStartTime(Instant.parse("2026-11-20T10:00:00Z"));
        response.setEndTime(Instant.parse("2026-11-20T12:00:00Z"));
        response.setReason("Congé");
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
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
        verifyNoInteractions(availabilityService);
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
        when(availabilityService.addUnavailability(eq("w1"), any())).thenReturn(sampleResponse());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("u1"))
                .andExpect(jsonPath("$.reason").value("Congé"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withoutReason_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"2026-11-20T10:00:00Z\",\"endTime\":\"2026-11-20T12:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("reason: Reason is required"));
        verifyNoInteractions(availabilityService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withoutStartTime_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endTime\":\"2026-11-20T12:00:00Z\",\"reason\":\"Congé\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startTime: Start time is required"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withStartAfterEnd_isBadRequest() throws Exception {
        when(availabilityService.addUnavailability(eq("w1"), any()))
                .thenThrow(new RuntimeException("Start time must be before end time."));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start time must be before end time."));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_forUnknownWorker_isNotFound() throws Exception {
        when(availabilityService.addUnavailability(eq("w1"), any()))
                .thenThrow(new ResourceNotFoundException("Worker not found with id: w1"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Worker not found with id: w1"));
    }

    // --- list / edit / delete ---

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void list_returnsIntervals() throws Exception {
        when(availabilityService.getUnavailabilities("w1")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void update_returns200() throws Exception {
        when(availabilityService.updateUnavailability(eq("w1"), eq("u1"), any())).thenReturn(sampleResponse());

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
        verify(availabilityService).deleteUnavailability("w1", "u1");
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void delete_unknownInterval_isNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Unavailability not found with id: u9 for worker: w1"))
                .when(availabilityService).deleteUnavailability("w1", "u9");

        mockMvc.perform(delete(URL + "/u9")).andExpect(status().isNotFound());
    }

    // --- unavailable workers ---

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void unavailableWorkers_parsesIsoPeriod() throws Exception {
        Instant start = Instant.parse("2026-11-20T08:00:00Z");
        Instant end = Instant.parse("2026-11-20T18:00:00Z");
        WorkerResponse worker = new WorkerResponse();
        worker.setId("w1");
        when(availabilityService.getUnavailableWorkers(start, end)).thenReturn(List.of(worker));

        mockMvc.perform(get("/api/logistics/workers/unavailable")
                        .param("start", "2026-11-20T08:00:00Z")
                        .param("end", "2026-11-20T18:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("w1"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void unavailableWorkers_withoutEnd_isBadRequest() throws Exception {
        mockMvc.perform(get("/api/logistics/workers/unavailable").param("start", "2026-11-20T08:00:00Z"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(availabilityService);
    }
}
