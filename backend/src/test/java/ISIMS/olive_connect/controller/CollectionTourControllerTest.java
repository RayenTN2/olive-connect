package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.CollectionTourResponse;
import ISIMS.olive_connect.exception.ResourceNotFoundException;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.security.AuthEntryPointJwt;
import ISIMS.olive_connect.security.JwtUtils;
import ISIMS.olive_connect.security.SecurityConfig;
import ISIMS.olive_connect.service.CollectionTourService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CollectionTourController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class})
class CollectionTourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionTourService tourService;

    // Required by SecurityConfig
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    private ResultActions changeLeader(String body) throws Exception {
        return mockMvc.perform(patch("/api/collection-tours/T/team-leader")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void anonymous_isUnauthorized() throws Exception {
        changeLeader("{\"teamLeaderId\":\"leila\"}").andExpect(status().isUnauthorized());
        verifyNoInteractions(tourService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void logisticsManager_isForbidden() throws Exception {
        changeLeader("{\"teamLeaderId\":\"leila\"}").andExpect(status().isForbidden());
        verifyNoInteractions(tourService);
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void collectionManager_changesTheLeader() throws Exception {
        CollectionTourResponse response = new CollectionTourResponse();
        response.setTeamLeaderId("leila");
        when(tourService.updateTeamLeader(eq("T"), argThat(r -> "leila".equals(r.getTeamLeaderId())))).thenReturn(response);

        changeLeader("{\"teamLeaderId\":\"leila\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamLeaderId").value("leila"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_isAllowed() throws Exception {
        changeLeader("{\"teamLeaderId\":\"leila\"}").andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void blankLeader_isBadRequest() throws Exception {
        changeLeader("{\"teamLeaderId\":\" \"}").andExpect(status().isBadRequest());
        verifyNoInteractions(tourService);
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void busyLeader_isBadRequestWithTheReason() throws Exception {
        String message = "Team leader 'Sami' is already assigned to an overlapping tour.";
        when(tourService.updateTeamLeader(any(), any())).thenThrow(new IllegalStateException(message));

        changeLeader("{\"teamLeaderId\":\"sami\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    // --- deleting a draft ---

    private ResultActions deleteTour() throws Exception {
        return mockMvc.perform(delete("/api/collection-tours/T"));
    }

    @Test
    void delete_anonymous_isUnauthorized() throws Exception {
        deleteTour().andExpect(status().isUnauthorized());
        verifyNoInteractions(tourService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void delete_logisticsManager_isForbidden() throws Exception {
        deleteTour().andExpect(status().isForbidden());
        verifyNoInteractions(tourService);
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void delete_collectionManager_returnsNoContent() throws Exception {
        deleteTour().andExpect(status().isNoContent());
        verify(tourService).deleteDraft("T");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_admin_returnsNoContent() throws Exception {
        deleteTour().andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void delete_confirmedTour_isBadRequest() throws Exception {
        String message = "Only PLANNED tours can be deleted. Current status: CONFIRMED";
        doThrow(new IllegalStateException(message)).when(tourService).deleteDraft("T");
        deleteTour().andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void delete_unknownTour_isNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Collection tour not found with id: T")).when(tourService).deleteDraft("T");
        deleteTour().andExpect(status().isNotFound());
    }
}
