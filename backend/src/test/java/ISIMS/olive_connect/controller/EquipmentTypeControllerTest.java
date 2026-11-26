package ISIMS.olive_connect.controller;

import ISIMS.olive_connect.dto.EquipmentTypeResponse;
import ISIMS.olive_connect.entity.EquipmentType;
import ISIMS.olive_connect.repository.UserRepository;
import ISIMS.olive_connect.security.AuthEntryPointJwt;
import ISIMS.olive_connect.security.JwtUtils;
import ISIMS.olive_connect.security.SecurityConfig;
import ISIMS.olive_connect.service.EquipmentService;
import ISIMS.olive_connect.service.EquipmentTypeService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// EquipmentController is loaded too, to prove /equipment/types is not captured by /equipment/{id}
@WebMvcTest({EquipmentTypeController.class, EquipmentController.class})
@Import({SecurityConfig.class, AuthEntryPointJwt.class})
class EquipmentTypeControllerTest {

    private static final String URL = "/api/logistics/equipment/types";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipmentTypeService equipmentTypeService;

    @MockitoBean
    private EquipmentService equipmentService;

    // Required by SecurityConfig
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(equipmentTypeService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_isAllowed() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"Trailer\"}"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void list_returnsTypesWithFields_andIsNotTreatedAsAnEquipmentId() throws Exception {
        when(equipmentTypeService.getTypes())
                .thenReturn(List.of(EquipmentTypeResponse.fromBuiltIn(EquipmentType.BIN)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BIN"))
                .andExpect(jsonPath("$[0].label").value("Bin"))
                .andExpect(jsonPath("$[0].builtIn").value(true))
                .andExpect(jsonPath("$[0].fields[0].name").value("capacityKg"))
                .andExpect(jsonPath("$[0].fields[0].required").value(true));
        verifyNoInteractions(equipmentService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_returns201() throws Exception {
        EquipmentTypeResponse created = new EquipmentTypeResponse();
        created.setCode("TRAILER_BIN");
        created.setLabel("Trailer bin");
        created.setFields(List.of());
        when(equipmentTypeService.createType(any())).thenReturn(created);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"Trailer bin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TRAILER_BIN"))
                .andExpect(jsonPath("$.builtIn").value(false));
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_withoutLabel_isBadRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("label: Label is required"));
        verifyNoInteractions(equipmentTypeService);
    }

    @Test
    @WithMockUser(roles = "LOGISTICS_MANAGER")
    void create_duplicate_isBadRequest() throws Exception {
        when(equipmentTypeService.createType(any()))
                .thenThrow(new RuntimeException("Equipment type already exists: BIN"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"bin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Equipment type already exists: BIN"));
    }
}
