package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.ResourceCalendarResponse;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.security.AuthEntryPointJwt;
import ISIMS.olive_connect.security.JwtUtils;
import ISIMS.olive_connect.security.SecurityConfig;
import ISIMS.olive_connect.service.ResourceCalendarService;
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

@WebMvcTest(ResourceCalendarController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class})
class ResourceCalendarControllerTest {

    private static final String URL = "/api/logistics/calendar";
    private static final String START = "2026-11-25T00:00:00Z";
    private static final String END = "2026-11-26T00:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResourceCalendarService calendarService;

    // Required by SecurityConfig
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get(URL).param("start", START).param("end", END)).andExpect(status().isUnauthorized());
        verifyNoInteractions(calendarService);
    }

    @Test
    @WithMockUser(roles = "COLLECTION_MANAGER")
    void collectionManager_isForbidden() throws Exception {
        mockMvc.perform(get(URL).param("start", START).param("end", END)).andExpect(status().isForbidden());
        verifyNoInteractions(calendarService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void logisticsManager_getsTheCalendar() throws Exception {
        ResourceCalendarResponse row = new ResourceCalendarResponse();
        row.setResourceType(ResourceCalendarResponse.ResourceType.WORKER);
        row.setId("w1");
        ResourceCalendarResponse.Entry entry = new ResourceCalendarResponse.Entry();
        entry.setKind(ResourceCalendarResponse.EntryKind.TOUR);
        entry.setTourId("A");
        entry.setConflict(true);
        row.setEntries(List.of(entry));
        when(calendarService.getCalendar(Instant.parse(START), Instant.parse(END))).thenReturn(List.of(row));

        mockMvc.perform(get(URL).param("start", START).param("end", END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceType").value("WORKER"))
                .andExpect(jsonPath("$[0].entries[0].kind").value("TOUR"))
                .andExpect(jsonPath("$[0].entries[0].tourId").value("A"))
                .andExpect(jsonPath("$[0].entries[0].conflict").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_getsTheCalendar() throws Exception {
        when(calendarService.getCalendar(any(), any())).thenReturn(List.of());
        mockMvc.perform(get(URL).param("start", START).param("end", END)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void missingStart_isBadRequest() throws Exception {
        mockMvc.perform(get(URL).param("end", END)).andExpect(status().isBadRequest());
        verifyNoInteractions(calendarService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void invalidPeriod_isBadRequest() throws Exception {
        when(calendarService.getCalendar(any(), any())).thenThrow(new RuntimeException("Start time must be before end time."));
        mockMvc.perform(get(URL).param("start", END).param("end", START)).andExpect(status().isBadRequest());
    }
}
