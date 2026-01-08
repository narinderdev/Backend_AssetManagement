package com.example.eam.InventoryManagement.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Enum.InventoryCategory;
import com.example.eam.Enum.UnitOfMeasure;
import com.example.eam.Exception.GlobalExceptionHandler;
import com.example.eam.InventoryManagement.Dto.InventoryItemCreateRequest;
import com.example.eam.InventoryManagement.Dto.InventoryItemResponse;
import com.example.eam.InventoryManagement.Dto.InventoryReorderCreateRequest;
import com.example.eam.InventoryManagement.Dto.InventoryReorderResponse;
import com.example.eam.InventoryManagement.Service.InventoryItemService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryItemControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private InventoryItemService service;

    @InjectMocks
    private InventoryItemController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void create_returnsCreated() throws Exception {
        InventoryItemResponse resp = InventoryItemResponse.builder()
                .id(1L)
                .itemId("ITEM-1")
                .itemName("Filter")
                .build();

        when(service.create(any(InventoryItemCreateRequest.class))).thenReturn(resp);

        InventoryItemCreateRequest req = new InventoryItemCreateRequest();
        req.setItemName("Filter");
        req.setCategory(InventoryCategory.FILTER);
        req.setUnitOfMeasure(UnitOfMeasure.EACH);
        req.setStockLevel(5);
        req.setReorderPoint(1);
        req.setReorderQuantity(2);
        req.setCostPerUnit(new BigDecimal("1.00"));
        req.setMinStockLevel(0);
        req.setMaxStockLevel(10);

        mockMvc.perform(post("/api/inventory-items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.itemName").value("Filter"));
    }

    @Test
    void list_returnsPage_directCall() {
        Page<InventoryItemResponse> page = new PageImpl<>(List.of(
                InventoryItemResponse.builder().itemId("ITEM-1").itemName("Filter").build()
        ));
        when(service.list(any())).thenReturn(page);

        var response = controller.list(org.springframework.data.domain.Pageable.unpaged());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<Page<InventoryItemResponse>> body = response.getBody();
        assertNotNull(body);
        assertEquals("success", body.getStatus());
    }

    @Test
    void reorder_badRequestHandled() throws Exception {
        when(service.reorder(any(Long.class), any(InventoryReorderCreateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "vendor missing"));

        InventoryReorderCreateRequest dto = new InventoryReorderCreateRequest();
        dto.setQuantity(1);

        mockMvc.perform(post("/api/inventory-items/1/reorder")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
