package com.example.eam.Technician.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Exception.GlobalExceptionHandler;
import com.example.eam.Technician.Dto.TechnicianCreateRequest;
import com.example.eam.Technician.Dto.TechnicianDetailsResponse;
import com.example.eam.Technician.Dto.TechnicianListResponse;
import com.example.eam.Technician.Service.TechnicianService;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TechnicianControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private TechnicianService technicianService;

    @InjectMocks
    private TechnicianController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        TechnicianDetailsResponse resp = TechnicianDetailsResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .technicianType(TechnicianType.FULL_TIME)
                .status(TechnicianStatus.ACTIVE)
                .build();
        when(technicianService.createTechnician(any(TechnicianCreateRequest.class))).thenReturn(resp);

        TechnicianCreateRequest req = new TechnicianCreateRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setTechnicianType(TechnicianType.FULL_TIME);

        mockMvc.perform(post("/api/technicians")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.firstName").value("John"));
    }

    @Test
    void list_returnsPage_directCall() {
        TechnicianDetailsResponse row = TechnicianDetailsResponse.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .technicianType(TechnicianType.CONTRACT)
                .status(TechnicianStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .build();
        TechnicianListResponse list = TechnicianListResponse.builder()
                .technicians(List.of(row))
                .page(0)
                .size(1)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();
        when(technicianService.listTechnicians(any())).thenReturn(list);

        var response = controller.list(org.springframework.data.domain.Pageable.unpaged());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<TechnicianListResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals("success", body.getStatus());
    }

    @Test
    void delete_conflictHandled() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "cannot delete"))
                .when(technicianService).deleteTechnician(any(Long.class));

        mockMvc.perform(delete("/api/technicians/9"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
