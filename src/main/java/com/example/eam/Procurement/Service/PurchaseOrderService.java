package com.example.eam.Procurement.Service;

import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.Procurement.Dto.*;
import com.example.eam.Procurement.Entity.MaterialRequisition;
import com.example.eam.Procurement.Entity.MaterialRequisitionLine;
import com.example.eam.Procurement.Entity.PurchaseOrder;
import com.example.eam.Procurement.Entity.PurchaseOrderLine;
import com.example.eam.Procurement.Enum.PurchaseOrderShipToType;
import com.example.eam.Procurement.Enum.MaterialRequisitionStatus;
import com.example.eam.Procurement.Enum.PurchaseOrderStatus;
import com.example.eam.Procurement.Repository.GoodsReceiptNoteRepository;
import com.example.eam.Procurement.Repository.MaterialRequisitionRepository;
import com.example.eam.Procurement.Repository.PurchaseOrderRepository;
import com.example.eam.Procurement.Repository.PurchaseOrderLineRepository;
import com.example.eam.VendorManagement.Entity.Vendor;
import com.example.eam.VendorManagement.Repository.VendorRepository;
import com.example.eam.InventoryManagement.Repository.WarehouseRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final MaterialRequisitionRepository mrRepository;
    private final VendorRepository vendorRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final GoodsReceiptNoteRepository grnRepository;
    private final WarehouseRepository warehouseRepository;
    private final WorkOrderRepository workOrderRepository;
    private final NumberGeneratorService numberGeneratorService;

    @Transactional
    public PurchaseOrderResponse convertMrToPo(Long mrId, ConvertMrToPoRequest request) {
        MaterialRequisition mr = mrRepository.findById(mrId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material Requisition not found"));
        if (mr.getStatus() != MaterialRequisitionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only APPROVED MRs can be converted to PO");
        }
        if (poRepository.existsByMrId(mrId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MR already converted to PO");
        }

        ShippingTarget shipping = resolveShippingTarget(
                request.getShipToType() != null ? request.getShipToType() : (mr.getShipToType() != null ? mr.getShipToType().name() : null),
                request.getShipToWarehouseId() != null ? request.getShipToWarehouseId() : mr.getShipToWarehouseId(),
                request.getShipToWorkOrderId() != null ? request.getShipToWorkOrderId() : mr.getShipToWorkOrderId());

        Vendor vendor = validateVendor(request.getVendorId());
        Map<Long, PoLineOverrideRequest> overrides = buildOverrideMap(request.getLineOverrides());
        if (!overrides.isEmpty()) {
            Set<Long> mrLineIds = Optional.ofNullable(mr.getLines())
                    .orElseGet(Collections::emptyList)
                    .stream()
                    .map(MaterialRequisitionLine::getId)
                    .collect(Collectors.toSet());
            for (Long overrideLineId : overrides.keySet()) {
                if (!mrLineIds.contains(overrideLineId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Override provided for non-existent MR line: " + overrideLineId);
                }
            }
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(numberGeneratorService.generatePoNumber())
                .vendorId(vendor.getId())
                .mrId(mr.getId())
                .department(trim(request.getDepartment() != null ? request.getDepartment() : mr.getDepartment()))
                .glAccountString(trim(request.getGlAccountString()))
                .shipToType(shipping.type)
                .shipToWarehouseId(shipping.warehouseId)
                .shipToWorkOrderId(shipping.workOrderId)
                .status(PurchaseOrderStatus.ISSUED)
                .requiredDeliveryDate(request.getRequiredDeliveryDate())
                .remarks(trim(request.getRemarks()))
                .createdByUserId(requireText(request.getCreatedByUserId(), "createdByUserId is required"))
                .build();

        for (MaterialRequisitionLine mrLine : Optional.ofNullable(mr.getLines()).orElseGet(Collections::emptyList)) {
            PoLineOverrideRequest override = overrides.get(mrLine.getId());
            BigDecimal qty = override != null && override.getOrderedQty() != null
                    ? override.getOrderedQty()
                    : mrLine.getRequestedQty();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderedQty must be greater than zero for MR line " + mrLine.getId());
            }
            BigDecimal unitPrice = override != null ? override.getUnitPrice() : null;
            if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unitPrice must be greater than zero for MR line " + mrLine.getId());
            }

            validateInventoryItemExists(mrLine.getItemId());

            String uom = override != null && override.getUom() != null ? trim(override.getUom()) : mrLine.getUom();
            String lineRemarks = override != null && override.getRemarks() != null ? trim(override.getRemarks()) : mrLine.getRemarks();

            po.addLine(PurchaseOrderLine.builder()
                    .po(po)
                    .itemId(mrLine.getItemId())
                    .orderedQty(qty.setScale(4, RoundingMode.HALF_UP))
                    .unitPrice(unitPrice != null ? unitPrice.setScale(4, RoundingMode.HALF_UP) : null)
                    .uom(uom)
                    .remarks(lineRemarks)
                    .build());
        }

        if (po.getLines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No lines available to create PO");
        }

        PurchaseOrder saved = poRepository.save(po);

        // mark MR as converted
        mr.setStatus(MaterialRequisitionStatus.CONVERTED_TO_PO);
        mr.setUpdatedAt(Instant.now());
        mrRepository.save(mr);

        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one PO line is required");
        }

        ShippingTarget shipping = resolveShippingTarget(request.getShipToType(), request.getShipToWarehouseId(), request.getShipToWorkOrderId());

        Vendor vendor = validateVendor(request.getVendorId());
        MaterialRequisition mr = null;
        if (request.getMrId() != null) {
            mr = mrRepository.findById(request.getMrId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material Requisition not found"));
            if (mr.getStatus() != MaterialRequisitionStatus.APPROVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Linked MR must be APPROVED");
            }
            if (poRepository.existsByMrId(request.getMrId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "MR already converted to PO");
            }
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(numberGeneratorService.generatePoNumber())
                .vendorId(vendor.getId())
                .mrId(mr != null ? mr.getId() : null)
                .department(trim(request.getDepartment()))
                .glAccountString(trim(request.getGlAccountString()))
                .shipToType(shipping.type)
                .shipToWarehouseId(shipping.warehouseId)
                .shipToWorkOrderId(shipping.workOrderId)
                .status(PurchaseOrderStatus.ISSUED)
                .requiredDeliveryDate(request.getRequiredDeliveryDate())
                .remarks(trim(request.getRemarks()))
                .createdByUserId(requireText(request.getCreatedByUserId(), "createdByUserId is required"))
                .build();

        for (PurchaseOrderLineRequest lineRequest : request.getLines()) {
            validateInventoryItemExists(lineRequest.getItemId());
            BigDecimal qty = lineRequest.getOrderedQty();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderedQty must be greater than zero");
            }
            BigDecimal unitPrice = lineRequest.getUnitPrice();
            if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unitPrice must be greater than zero");
            }
            po.addLine(PurchaseOrderLine.builder()
                    .po(po)
                    .itemId(lineRequest.getItemId())
                    .orderedQty(qty.setScale(4, RoundingMode.HALF_UP))
                    .unitPrice(unitPrice != null ? unitPrice.setScale(4, RoundingMode.HALF_UP) : null)
                    .uom(trim(lineRequest.getUom()))
                    .remarks(trim(lineRequest.getRemarks()))
                    .build());
        }

        return toResponse(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderResponse updateStatus(Long id, PurchaseOrderStatus newStatus, String remarks) {
        PurchaseOrder po = getOrThrow(id);

        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newStatus is required");
        }

        if (newStatus == po.getStatus()) {
            if (remarks != null) {
                po.setRemarks(trim(remarks));
                po.setUpdatedAt(Instant.now());
                poRepository.save(po);
            }
            return toResponse(po);
        }

        switch (newStatus) {
            case ISSUED -> {
                if (po.getStatus() == PurchaseOrderStatus.CANCELLED || po.getStatus() == PurchaseOrderStatus.CLOSED) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot issue PO from status " + po.getStatus());
                }
            }
            case DELIVERED -> {
                if (po.getStatus() == PurchaseOrderStatus.CANCELLED || po.getStatus() == PurchaseOrderStatus.CLOSED) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot mark Delivered from status " + po.getStatus());
                }
            }
            case CLOSED -> {
                if (po.getStatus() != PurchaseOrderStatus.DELIVERED) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DELIVERED PO can be closed");
                }
                if (!isFullyReceived(po)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "PO cannot be closed until all lines are fully received");
                }
            }
            case CANCELLED -> {
                if (grnRepository.existsByPoId(po.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "PO with GRNs cannot be cancelled");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status transition");
        }

        po.setStatus(newStatus);
        if (remarks != null) {
            po.setRemarks(trim(remarks));
        }
        po.setUpdatedAt(Instant.now());
        return toResponse(poRepository.save(po));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse get(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> list(Pageable pageable, Long mrId) {
        if (mrId != null) {
            List<PurchaseOrderResponse> responses = poRepository.findByMrId(mrId).stream()
                    .map(this::toResponse)
                    .toList();
            return new PageImpl<>(responses, pageable, responses.size());
        }
        return poRepository.findAll(pageable).map(this::toResponse);
    }

    // Helpers

    private Vendor validateVendor(Long vendorId) {
        if (vendorId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vendorId is required");
        }
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor not found: " + vendorId));
        if (!vendor.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor is inactive: " + vendorId);
        }
        return vendor;
    }

    private void validateInventoryItemExists(Long itemId) {
        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory item not found: " + itemId));
        if (!item.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory item is inactive: " + itemId);
        }
    }

    private Map<Long, PoLineOverrideRequest> buildOverrideMap(List<PoLineOverrideRequest> overrides) {
        if (overrides == null) return Collections.emptyMap();
        try {
            return overrides.stream()
                    .collect(Collectors.toMap(PoLineOverrideRequest::getMrLineId, Function.identity()));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate MR line overrides provided");
        }
    }

    private boolean isFullyReceived(PurchaseOrder po) {
        return Optional.ofNullable(po.getLines())
                .orElseGet(Collections::emptyList)
                .stream()
                .allMatch(line -> line.getOrderedQty().compareTo(line.getReceivedQty()) == 0);
    }

    private PurchaseOrder getOrThrow(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase Order not found"));
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        List<PurchaseOrderLineResponse> lines = Optional.ofNullable(po.getLines())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(line -> PurchaseOrderLineResponse.builder()
                        .id(line.getId())
                        .itemId(line.getItemId())
                        .orderedQty(line.getOrderedQty())
                        .receivedQty(line.getReceivedQty())
                        .unitPrice(line.getUnitPrice())
                        .uom(line.getUom())
                        .remarks(line.getRemarks())
                        .build())
                .toList();

        Vendor vendor = null;
        if (po.getVendorId() != null) {
            vendor = vendorRepository.findById(po.getVendorId()).orElse(null);
        }

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .vendorId(po.getVendorId())
                .vendorCode(vendor != null ? vendor.getVendorId() : null)
                .vendorName(vendor != null ? vendor.getVendorName() : null)
                .vendorEmail(vendor != null ? vendor.getEmail() : null)
                .vendorPhone(vendor != null ? vendor.getPhone() : null)
                .vendorContactPerson(vendor != null ? vendor.getContactPerson() : null)
                .mrId(po.getMrId())
                .department(po.getDepartment())
                .shipToType(po.getShipToType() != null ? po.getShipToType().name() : null)
                .shipToWarehouseId(po.getShipToWarehouseId())
                .shipToWorkOrderId(po.getShipToWorkOrderId())
                .status(po.getStatus())
                .glAccountString(po.getGlAccountString())
                .requiredDeliveryDate(po.getRequiredDeliveryDate())
                .remarks(po.getRemarks())
                .createdByUserId(po.getCreatedByUserId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .lines(lines)
                .build();
    }

    private String trim(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private String requireText(String value, String message) {
        String t = trim(value);
        if (t == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return t;
    }

    private ShippingTarget resolveShippingTarget(String shipToTypeRaw, Long warehouseId, Long workOrderId) {
        if (shipToTypeRaw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shipToType is required (WAREHOUSE or WORK_SITE)");
        }
        PurchaseOrderShipToType type;
        try {
            type = PurchaseOrderShipToType.valueOf(shipToTypeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipToType. Allowed: WAREHOUSE, WORK_SITE");
        }

        Long resolvedWarehouseId = null;
        Long resolvedWorkOrderId = null;

        if (type == PurchaseOrderShipToType.WAREHOUSE) {
            if (warehouseId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "warehouseId is required when shipToType=WAREHOUSE");
            }
            var warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse not found"));
            if (!warehouse.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse is inactive");
            }
            resolvedWarehouseId = warehouse.getId();
        } else {
            if (workOrderId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workOrderId is required when shipToType=WORK_SITE");
            }
            workOrderRepository.findByIdAndDeletedFalse(workOrderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Work order not found"));
            resolvedWorkOrderId = workOrderId;
        }

        return new ShippingTarget(type, resolvedWarehouseId, resolvedWorkOrderId);
    }

    private record ShippingTarget(PurchaseOrderShipToType type, Long warehouseId, Long workOrderId) { }
}
