package com.example.eam.ServiceMaintenance.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Exception.GlobalExceptionHandler;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestApproveDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestCreateDto;
import com.example.eam.ServiceMaintenance.Dto.ServiceRequestResponse;
import com.example.eam.ServiceMaintenance.Service.ServiceMaintenanceService;
import com.example.eam.WorkOrder.Dto.WorkOrderDetailsResponse;
import com.example.eam.WorkOrder.Service.WorkOrderService;
import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.Enum.ServiceRequestStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServiceMaintenanceControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ServiceMaintenanceService service;

    @Mock
    private WorkOrderService workOrderService;

    @InjectMocks
    private ServiceMaintenanceController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        ServiceRequestResponse response = ServiceRequestResponse.builder()
                .id(1L)
                .requestId("SR-000001")
                .status(ServiceRequestStatus.NEW)
                .build();
        when(service.create(any(ServiceRequestCreateDto.class))).thenReturn(response);

        ServiceRequestCreateDto dto = new ServiceRequestCreateDto();
        dto.setRequesterName("Ops");
        dto.setMaintenanceType(MaintenanceType.CORRECTIVE);
        dto.setPriority(RequestPriority.MEDIUM);
        dto.setShortTitle("Leak");
        dto.setProblemDescription("Pipe leak");

        mockMvc.perform(post("/api/service-requests")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requestId").value("SR-000001"));
    }

    @Test
    void approve_returnsOk() throws Exception {
        ServiceRequestResponse response = ServiceRequestResponse.builder()
                .id(2L)
                .status(ServiceRequestStatus.APPROVED)
                .build();
        when(service.approve(any(Long.class), any(ServiceRequestApproveDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/service-requests/2/approve")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void convertToWo_returnsCreated() throws Exception {
        WorkOrderDetailsResponse wo = WorkOrderDetailsResponse.builder()
                .id(5L)
                .workOrderId("WO-1")
                .build();
        when(workOrderService.convertServiceRequestToWorkOrder(3L)).thenReturn(wo);

        mockMvc.perform(post("/api/service-requests/3/convert-to-wo"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.workOrderId").value("WO-1"));
    }

    @Test
    void list_returnsPage_directCall() {
        when(service.list(any())).thenReturn(new PageImpl<>(List.of(
                ServiceRequestResponse.builder().requestId("SR-1").build()
        )));

        var response = controller.list(org.springframework.data.domain.Pageable.unpaged());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<org.springframework.data.domain.Page<ServiceRequestResponse>> body = response.getBody();
        assertNotNull(body);
        assertEquals("success", body.getStatus());
    }

    @Test
    void reject_conflictHandled() throws Exception {
        when(service.reject(any(Long.class), any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "already approved"));

        mockMvc.perform(post("/api/service-requests/10/reject")
                        .contentType("application/json")
                        .content("""
                                {"reason":"dup"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"));

        verify(service).reject(any(Long.class), any());
    }
}
