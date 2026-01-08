package com.example.eam.Roles.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Exception.GlobalExceptionHandler;
import com.example.eam.Roles.Dto.RoleCreateRequest;
import com.example.eam.Roles.Dto.RolePatchRequest;
import com.example.eam.Roles.Dto.RoleResponse;
import com.example.eam.Roles.Service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        RoleResponse response = RoleResponse.builder()
                .id(1L)
                .name("Viewer")
                .permissionCodes(Set.of("READ"))
                .build();
        when(roleService.create(any(RoleCreateRequest.class))).thenReturn(response);

        RoleCreateRequest req = new RoleCreateRequest();
        req.setName("Viewer");
        req.setPermissionCodes(Set.of("READ"));

        mockMvc.perform(post("/api/roles")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Viewer"));
    }

    @Test
    void patch_conflictHandled() throws Exception {
        when(roleService.patch(any(Long.class), any(RolePatchRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "dup"));

        mockMvc.perform(patch("/api/roles/1")
                        .contentType("application/json")
                        .content("""
                                {"name":"Admin"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void list_returnsPage_directCall() {
        when(roleService.list(any())).thenReturn(new PageImpl<>(List.of(
                RoleResponse.builder().name("Viewer").build()
        )));

        var response = controller.list(org.springframework.data.domain.Pageable.unpaged());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<org.springframework.data.domain.Page<RoleResponse>> body = response.getBody();
        assertNotNull(body);
        assertEquals("success", body.getStatus());
    }
}
