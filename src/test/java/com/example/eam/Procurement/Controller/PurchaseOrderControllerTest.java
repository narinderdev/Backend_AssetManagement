package com.example.eam.Procurement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Exception.GlobalExceptionHandler;
import com.example.eam.Procurement.Dto.CreatePurchaseOrderRequest;
import com.example.eam.Procurement.Dto.PurchaseOrderLineRequest;
import com.example.eam.Procurement.Dto.PurchaseOrderResponse;
import com.example.eam.Procurement.Enum.PurchaseOrderStatus;
import com.example.eam.Procurement.Service.PurchaseOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class PurchaseOrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @InjectMocks
    private PurchaseOrderController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        PurchaseOrderResponse response = PurchaseOrderResponse.builder()
                .id(1L)
                .poNumber("PO-1")
                .status(PurchaseOrderStatus.DRAFT)
                .build();

        when(purchaseOrderService.create(any(CreatePurchaseOrderRequest.class))).thenReturn(response);

        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .vendorId(10L)
                .createdByUserId("user1")
                .expectedDeliveryDate(LocalDate.now())
                .lines(List.of(PurchaseOrderLineRequest.builder()
                        .itemId(5L)
                        .orderedQty(new BigDecimal("2.0"))
                        .unitPrice(new BigDecimal("10.0"))
                        .uom("EACH")
                        .build()))
                .build();

        mockMvc.perform(post("/api/procurement/po")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.poNumber").value("PO-1"));
    }

    @Test
    void list_returnsPage_directCall() {
        Page<PurchaseOrderResponse> page = new PageImpl<>(List.of(
                PurchaseOrderResponse.builder().id(1L).poNumber("PO-1").status(PurchaseOrderStatus.DRAFT).build()
        ));
        when(purchaseOrderService.list(any(), any())).thenReturn(page);

        var response = controller.list(org.springframework.data.domain.Pageable.unpaged(), null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<Page<PurchaseOrderResponse>> body = response.getBody();
        assertNotNull(body);
        assertEquals("success", body.getStatus());
    }

    @Test
    void updateStatus_handlesConflict() throws Exception {
        when(purchaseOrderService.updateStatus(any(Long.class), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "invalid transition"));

        mockMvc.perform(patch("/api/procurement/po/1/status")
                        .contentType("application/json")
                        .content("""
                                {"newStatus":"DELIVERED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"));

        verify(purchaseOrderService).updateStatus(any(Long.class), any(), any());
    }
}
