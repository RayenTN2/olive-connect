package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.WorkerResponse;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.security.AuthEntryPointJwt;
import ISIMS.olive_connect.security.JwtUtils;
import ISIMS.olive_connect.security.SecurityConfig;
import ISIMS.olive_connect.service.WorkerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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

// Loads only the web layer with the real SecurityConfig, so URL rules and @PreAuthorize are both exercised.
@WebMvcTest(WorkerController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class})
class WorkerControllerTest {

    private static final String URL = "/api/logistics/workers";
    private static final String VALID_BODY = "{\"fullName\":\"Ali Ben Salah\",\"phone\":\"+21698123456\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkerService workerService;

    // Required by SecurityConfig
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    private WorkerResponse sampleResponse(boolean active) {
        WorkerResponse response = new WorkerResponse();
        response.setId("w1");
        response.setFullName("Ali Ben Salah");
        response.setPhone("+21698123456");
        response.setActive(active);
        return response;
    }

    @Test
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(workerService);
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void otherRole_isForbidden() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
        verifyNoInteractions(workerService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_isForbidden() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(workerService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void logisticsManager_createsWorker() throws Exception {
        when(workerService.createWorker(any())).thenReturn(sampleResponse(true));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("w1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withBlankFullName_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\" \",\"phone\":\"+21698123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fullName: Full name is required"));
        verifyNoInteractions(workerService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withoutPhone_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ali Ben Salah\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("phone: Phone number is required"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void list_passesActiveFilter() throws Exception {
        when(workerService.getWorkers(true)).thenReturn(List.of(sampleResponse(true)));

        mockMvc.perform(get(URL).param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(workerService).getWorkers(true);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void update_returnsUpdatedWorker() throws Exception {
        when(workerService.updateWorker(eq("w1"), any())).thenReturn(sampleResponse(true));

        mockMvc.perform(put(URL + "/w1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ali Ben Salah"));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void deactivate_returnsInactiveWorker() throws Exception {
        when(workerService.updateWorkerStatus("w1", false)).thenReturn(sampleResponse(false));

        mockMvc.perform(patch(URL + "/w1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void status_withoutActiveValue_isBadRequest() throws Exception {
        mockMvc.perform(patch(URL + "/w1/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("active: Active status is required"));
        verifyNoInteractions(workerService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void unknownWorker_isNotFound() throws Exception {
        when(workerService.updateWorkerStatus("missing", false))
                .thenThrow(new ResourceNotFoundException("Worker not found with id: missing"));

        mockMvc.perform(patch(URL + "/missing/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Worker not found with id: missing"));
    }
}
