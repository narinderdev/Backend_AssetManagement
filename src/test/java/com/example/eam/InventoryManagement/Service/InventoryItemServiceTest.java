package com.example.eam.InventoryManagement.Service;

import com.example.eam.Enum.ReorderStatus;
import com.example.eam.InventoryManagement.Dto.InventoryItemCreateRequest;
import com.example.eam.InventoryManagement.Dto.InventoryReorderCreateRequest;
import com.example.eam.InventoryManagement.Dto.InventoryReorderResponse;
import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Entity.InventoryReorderRequest;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.InventoryManagement.Repository.InventoryReorderRequestRepository;
import com.example.eam.VendorManagement.Entity.Vendor;
import com.example.eam.VendorManagement.Repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

    @Mock
    private InventoryItemRepository itemRepo;
    @Mock
    private VendorRepository vendorRepo;
    @Mock
    private InventoryReorderRequestRepository reorderRepo;

    @InjectMocks
    private InventoryItemService service;

    @Test
    void create_generatesIdAndMarksActive() {
        when(itemRepo.existsByItemId(any())).thenReturn(false);
        when(itemRepo.save(any())).thenAnswer(invocation -> {
            InventoryItem item = invocation.getArgument(0);
            item.setId(10L);
            return item;
        });

        InventoryItemCreateRequest dto = new InventoryItemCreateRequest();
        dto.setItemName("Filter");
        dto.setCategory(com.example.eam.Enum.InventoryCategory.FILTER);
        dto.setUnitOfMeasure(com.example.eam.Enum.UnitOfMeasure.EACH);
        dto.setStockLevel(5);
        dto.setReorderQuantity(2);
        dto.setReorderPoint(1);
        dto.setMinStockLevel(0);
        dto.setMaxStockLevel(10);
        dto.setCostPerUnit(new BigDecimal("3.50"));

        var resp = service.create(dto);

        assertNotNull(resp.getItemId());
        assertEquals("Filter", resp.getItemName());
        assertEquals(true, resp.isActive());
    }

    @Test
    void reorder_missingVendorEmail_rejected() {
        InventoryItem item = InventoryItem.builder()
                .id(1L)
                .itemId("ITEM-1")
                .itemName("Filter")
                .reorderQuantity(2)
                .costPerUnit(new BigDecimal("5.00"))
                .primaryVendor(Vendor.builder().id(2L).vendorName("NoEmail").email("").build())
                .build();

        when(itemRepo.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(item));

        InventoryReorderCreateRequest dto = new InventoryReorderCreateRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.reorder(1L, dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void reorder_usesProvidedVendor() {
        InventoryItem item = InventoryItem.builder()
                .id(1L)
                .itemId("ITEM-1")
                .itemName("Filter")
                .reorderQuantity(2)
                .costPerUnit(new BigDecimal("5.00"))
                .build();

        Vendor vendor = Vendor.builder()
                .id(2L)
                .vendorName("Acme")
                .email("acme@example.com")
                .build();

        lenient().when(reorderRepo.existsByReorderId(any())).thenReturn(false);
        when(itemRepo.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(item));
        when(vendorRepo.findById(2L)).thenReturn(Optional.of(vendor));
        when(reorderRepo.save(any())).thenAnswer(invocation -> {
            InventoryReorderRequest req = invocation.getArgument(0);
            req.setId(5L);
            req.setStatus(ReorderStatus.CREATED);
            return req;
        });

        InventoryReorderCreateRequest dto = new InventoryReorderCreateRequest();
        dto.setVendorDbId(2L);
        dto.setQuantity(3);

        InventoryReorderResponse resp = service.reorder(1L, dto);

        assertEquals("Acme", resp.getVendorName());
        assertEquals(3, resp.getQuantity());
        assertEquals(ReorderStatus.CREATED, resp.getStatus());
    }
}
