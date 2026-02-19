package com.example.eam.Procurement.Service;

import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.InventoryManagement.Service.InventoryAuditLogService;
import com.example.eam.Procurement.Dto.VendorReturnResponse;
import com.example.eam.Procurement.Entity.GoodsReceiptNote;
import com.example.eam.Procurement.Entity.GoodsReceiptNoteLine;
import com.example.eam.Procurement.Entity.PurchaseOrderLine;
import com.example.eam.Procurement.Entity.VendorReturn;
import com.example.eam.Procurement.Repository.GoodsReceiptNoteLineRepository;
import com.example.eam.Procurement.Repository.PurchaseOrderLineRepository;
import com.example.eam.Procurement.Repository.VendorReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorReturnService {

    private final VendorReturnRepository vendorReturnRepository;
    private final GoodsReceiptNoteLineRepository grnLineRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryAuditLogService inventoryAuditLogService;

    @Transactional
    public void recordInitialReturns(GoodsReceiptNote grn) {
        if (grn == null || grn.getLines() == null || grn.getLines().isEmpty()) return;
        for (GoodsReceiptNoteLine line : grn.getLines()) {
            if (line.getReturnQty() == null || line.getReturnQty().compareTo(BigDecimal.ZERO) <= 0) continue;
            recordReturnForGrnLine(line, grn);
        }
    }

    @Transactional
    private VendorReturnResponse recordReturnForGrnLine(GoodsReceiptNoteLine line, GoodsReceiptNote grn) {
        BigDecimal returnQty = line.getReturnQty();
        if (returnQty == null || returnQty.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        returnQty = normalizeQty(returnQty, "returnQty");

        BigDecimal receivedQty = line.getReceivedQty() != null ? line.getReceivedQty() : BigDecimal.ZERO;
        if (returnQty.compareTo(receivedQty) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Return exceeds received quantity");
        }

        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(line.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found for GRN line"));

        int deltaUnits = toWholeUnits(returnQty);
        int stockBefore = item.getStockLevel() != null ? item.getStockLevel() : 0;
        if (stockBefore < deltaUnits) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock to return");
        }
        int stockAfter = stockBefore - deltaUnits;

        BigDecimal unitCost = resolveUnitCost(line, grn, item);
        BigDecimal returnCost = unitCost != null
                ? unitCost.multiply(returnQty).setScale(2, RoundingMode.HALF_UP)
                : null;

        item.setStockLevel(stockAfter);
        inventoryItemRepository.save(item);

        VendorReturn saved = vendorReturnRepository.save(VendorReturn.builder()
                .grnId(grn.getId())
                .grnLineId(line.getId())
                .poId(grn.getPoId())
                .poLineId(line.getPoLineId())
                .vendorId(grn.getVendorId())
                .itemId(line.getItemId())
                .returnQty(returnQty)
                .unitCostSnapshot(unitCost)
                .returnCost(returnCost)
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
                .reason(grn.getNotes())
                .performedBy(grn.getReceivedByUserId())
                .build());

        inventoryAuditLogService.recordReturn(
                item,
                stockBefore,
                stockAfter,
                grn.getPoId() != null ? InventoryReferenceType.PURCHASE_ORDER : InventoryReferenceType.OTHER,
                grn.getGrnNumber() != null ? grn.getGrnNumber() : "GRN-" + grn.getId(),
                saved.getPerformedBy(),
                saved.getReason()
        );

        return toResponse(saved, line, grn, item);
    }

    @Transactional(readOnly = true)
    public List<VendorReturnResponse> list(Long grnId, Long vendorId, Long itemId) {
        List<VendorReturn> returns = vendorReturnRepository.findByFilters(grnId, vendorId, itemId);
        if (returns.isEmpty()) return List.of();

        Map<Long, GoodsReceiptNoteLine> linesById = grnLineRepository.findAllById(
                        returns.stream().map(VendorReturn::getGrnLineId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(GoodsReceiptNoteLine::getId, l -> l));

        Map<Long, InventoryItem> itemsById = inventoryItemRepository.findAllById(
                        returns.stream().map(VendorReturn::getItemId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(InventoryItem::getId, i -> i));

        List<VendorReturnResponse> responses = new ArrayList<>(returns.size());
        for (VendorReturn r : returns) {
            GoodsReceiptNoteLine line = linesById.get(r.getGrnLineId());
            GoodsReceiptNote grn = line != null ? line.getGrn() : null;
            InventoryItem item = itemsById.get(r.getItemId());
            responses.add(toResponse(r, line, grn, item));
        }
        return responses;
    }

    private VendorReturnResponse toResponse(VendorReturn saved,
                                            GoodsReceiptNoteLine line,
                                            GoodsReceiptNote grn,
                                            InventoryItem item) {
        BigDecimal orderedQty = line != null ? line.getOrderedQty() : null;
        BigDecimal receivedQty = line != null ? line.getReceivedQty() : null;
        BigDecimal totalReturned = line != null && line.getReturnQty() != null ? line.getReturnQty() : null;

        return VendorReturnResponse.builder()
                .id(saved.getId())
                .grnId(saved.getGrnId())
                .grnNumber(grn != null ? grn.getGrnNumber() : null)
                .grnLineId(saved.getGrnLineId())
                .poId(saved.getPoId())
                .poLineId(saved.getPoLineId())
                .vendorId(saved.getVendorId())
                .itemDbId(item != null ? item.getId() : saved.getItemId())
                .itemId(item != null ? item.getItemId() : null)
                .skuNumber(item != null ? item.getSkuNumber() : null)
                .itemName(item != null ? item.getItemName() : null)
                .orderedQty(orderedQty)
                .receivedQty(receivedQty)
                .returnQty(saved.getReturnQty())
                .totalReturnedQty(totalReturned)
                .unitCost(saved.getUnitCostSnapshot())
                .returnCost(saved.getReturnCost())
                .stockBefore(saved.getStockBefore())
                .stockAfter(saved.getStockAfter())
                .reason(saved.getReason())
                .performedBy(saved.getPerformedBy())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private BigDecimal resolveUnitCost(GoodsReceiptNoteLine line, GoodsReceiptNote grn, InventoryItem item) {
        if (line.getPoLineId() != null && grn.getPoId() != null) {
            Optional<PurchaseOrderLine> poLine = poLineRepository.findByIdAndPoId(line.getPoLineId(), grn.getPoId());
            if (poLine.isPresent() && poLine.get().getUnitPrice() != null) {
                return poLine.get().getUnitPrice().setScale(4, RoundingMode.HALF_UP);
            }
        }
        if (item.getCostPerUnit() != null) {
            return item.getCostPerUnit().setScale(4, RoundingMode.HALF_UP);
        }
        return null;
    }

    private int toWholeUnits(BigDecimal qty) {
        BigDecimal normalized = qty.stripTrailingZeros();
        if (normalized.scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only whole units supported for stock");
        }
        try {
            return normalized.intValueExact();
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "returnQty is too large for stock counter");
        }
    }

    private BigDecimal normalizeQty(BigDecimal qty, String field) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be greater than zero");
        }
        return qty.setScale(4, RoundingMode.HALF_UP);
    }

    private String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
