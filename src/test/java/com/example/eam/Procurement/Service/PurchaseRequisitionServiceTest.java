package com.example.eam.Procurement.Service;

import com.example.eam.Asset.Entity.Asset;
import com.example.eam.Asset.Repository.AssetRepository;
import com.example.eam.Enum.PrLineType;
import com.example.eam.Enum.PrStatus;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.Procurement.Dto.PrCreateRequest;
import com.example.eam.Procurement.Dto.PrDetailsResponse;
import com.example.eam.Procurement.Dto.PrLineCreateDto;
import com.example.eam.Procurement.Repository.PurchaseRequisitionRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRequisitionServiceTest {

    @Mock
    private PurchaseRequisitionRepository prRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private PurchaseRequisitionService service;

    @Test
    void create_withPartLine_buildsTotals() {
        InventoryItem item = InventoryItem.builder()
                .id(5L)
                .itemId("ITEM-1")
                .itemName("Bolt")
                .build();

        lenient().when(prRepository.existsByPrId(any())).thenReturn(false);
        when(inventoryItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(prRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PrLineCreateDto line = new PrLineCreateDto();
        line.setLineType(PrLineType.PART);
        line.setInventoryItemDbId(5L);
        line.setQtyRequested(2);
        line.setEstimatedUnitPrice(new BigDecimal("10.00"));

        PrCreateRequest request = new PrCreateRequest();
        request.setRequester("Jane");
        request.setRequiredByDate(LocalDate.now().plusDays(2));
        request.setPriority(PriorityLevel.MEDIUM);
        request.setLines(List.of(line));

        PrDetailsResponse response = service.create(request);

        assertNotNull(response.getPrId());
        assertEquals(PrStatus.DRAFT, response.getStatus());
        assertEquals(1, response.getLines().size());
        assertEquals(new BigDecimal("20.00"), response.getTotalEstimatedCost());
    }

    @Test
    void create_withCriticalPriority_rejected() {
        PrCreateRequest request = new PrCreateRequest();
        request.setRequester("Jane");
        request.setRequiredByDate(LocalDate.now());
        request.setPriority(PriorityLevel.CRITICAL);

        PrLineCreateDto line = new PrLineCreateDto();
        line.setLineType(PrLineType.SERVICE);
        line.setQtyRequested(1);
        line.setDescription("Support");
        request.setLines(List.of(line));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
