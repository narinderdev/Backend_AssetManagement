package com.example.eam.Procurement.Service;

import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.Procurement.Dto.*;
import com.example.eam.Procurement.Entity.MaterialRequisition;
import com.example.eam.Procurement.Entity.MaterialRequisitionLine;
import com.example.eam.Procurement.Entity.PurchaseOrder;
import com.example.eam.Procurement.Enum.MaterialRequisitionStatus;
import com.example.eam.Procurement.Enum.MaterialRequisitionShipToType;
import com.example.eam.Procurement.Repository.GoodsReceiptNoteRepository;
import com.example.eam.Procurement.Repository.MaterialRequisitionRepository;
import com.example.eam.Procurement.Repository.PurchaseOrderRepository;
import com.example.eam.InventoryManagement.Repository.WarehouseRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
public class MaterialRequisitionService {

    private final MaterialRequisitionRepository mrRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final WorkOrderRepository workOrderRepository;
    private final NumberGeneratorService numberGeneratorService;
    private final PurchaseOrderRepository poRepository;
    private final GoodsReceiptNoteRepository grnRepository;

    @Transactional
    public MaterialRequisitionResponse create(CreateMaterialRequisitionRequest request) {
        validateLines(request.getLines());
        ShippingTarget shipping = resolveShippingTarget(request.getShipToType(), request.getShipToWarehouseId(), request.getShipToWorkOrderId());

        String mrNumber = numberGeneratorService.generateMrNumber();
        MaterialRequisition mr = MaterialRequisition.builder()
                .mrNumber(mrNumber)
                .requestedByUserId(requireText(request.getRequestedByUserId(), "requestedByUserId is required"))
                .neededByDate(request.getNeededByDate())
                .notes(trim(request.getNotes()))
                .department(trim(request.getDepartment()))
                .shipToType(shipping.type)
                .shipToWarehouseId(shipping.warehouseId)
                .shipToWorkOrderId(shipping.workOrderId)
                // Direct submission on create as requested
                .status(MaterialRequisitionStatus.SUBMITTED)
                .build();

        for (MaterialRequisitionLineRequest lineRequest : request.getLines()) {
            mr.addLine(buildLine(mr, lineRequest));
        }

        return toResponse(mrRepository.save(mr));
    }

    @Transactional
    public MaterialRequisitionResponse update(Long id, UpdateMaterialRequisitionRequest request) {
        MaterialRequisition mr = getOrThrow(id);
        if (mr.getStatus() == MaterialRequisitionStatus.APPROVED
                || mr.getStatus() == MaterialRequisitionStatus.REJECTED
                || mr.getStatus() == MaterialRequisitionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MR cannot be updated in status " + mr.getStatus());
        }

        if (request.getRequestedByUserId() != null) {
            mr.setRequestedByUserId(requireText(request.getRequestedByUserId(), "requestedByUserId cannot be blank"));
        }
        if (request.getNeededByDate() != null) {
            mr.setNeededByDate(request.getNeededByDate());
        }
        if (request.getNotes() != null) {
            mr.setNotes(trim(request.getNotes()));
        }
        if (request.getDepartment() != null) {
            mr.setDepartment(trim(request.getDepartment()));
        }
        if (request.getShipToType() != null || request.getShipToWarehouseId() != null || request.getShipToWorkOrderId() != null) {
            ShippingTarget shipping = resolveShippingTarget(request.getShipToType(), request.getShipToWarehouseId(), request.getShipToWorkOrderId());
            mr.setShipToType(shipping.type);
            mr.setShipToWarehouseId(shipping.warehouseId);
            mr.setShipToWorkOrderId(shipping.workOrderId);
        }

        if (request.getLines() != null) {
            validateLines(request.getLines());
            mr.getLines().clear();
            for (MaterialRequisitionLineRequest lineRequest : request.getLines()) {
                mr.addLine(buildLine(mr, lineRequest));
            }
        }

        mr.setUpdatedAt(Instant.now());
        return toResponse(mrRepository.save(mr));
    }

    @Transactional
    public MaterialRequisitionResponse submit(Long id) {
        MaterialRequisition mr = getOrThrow(id);
        if (mr.getLines() == null || mr.getLines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material Requisition requires at least one line");
        }
        if (mr.getStatus() == MaterialRequisitionStatus.SUBMITTED) {
            return toResponse(mr);
        }
        if (mr.getStatus() == MaterialRequisitionStatus.DRAFT) {
            mr.setStatus(MaterialRequisitionStatus.SUBMITTED);
            mr.setUpdatedAt(Instant.now());
            return toResponse(mrRepository.save(mr));
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "MR cannot be submitted in status " + mr.getStatus());
    }

    @Transactional
    public MaterialRequisitionResponse approve(Long id, MrApprovalRequest request) {
        MaterialRequisition mr = getOrThrow(id);
        if (mr.getStatus() != MaterialRequisitionStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUBMITTED MRs can be approved");
        }
        mr.setStatus(MaterialRequisitionStatus.APPROVED);
        mr.setApprovedByUserId(requireText(request.getApprovedByUserId(), "approvedByUserId is required"));
        mr.setApprovedAt(Instant.now());
        mr.setRejectedAt(null);
        mr.setRejectedByUserId(null);
        mr.setRejectionReason(null);
        mr.setUpdatedAt(Instant.now());
        return toResponse(mrRepository.save(mr));
    }

    @Transactional
    public MaterialRequisitionResponse reject(Long id, MrRejectionRequest request) {
        MaterialRequisition mr = getOrThrow(id);
        if (mr.getStatus() != MaterialRequisitionStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUBMITTED MRs can be rejected");
        }
        mr.setStatus(MaterialRequisitionStatus.REJECTED);
        mr.setRejectedByUserId(requireText(request.getRejectedByUserId(), "rejectedByUserId is required"));
        mr.setRejectionReason(requireText(request.getReason(), "reason is required"));
        mr.setRejectedAt(Instant.now());
        mr.setUpdatedAt(Instant.now());
        return toResponse(mrRepository.save(mr));
    }

    @Transactional
    public MaterialRequisitionResponse cancel(Long id) {
        MaterialRequisition mr = getOrThrow(id);
        if (mr.getStatus() == MaterialRequisitionStatus.APPROVED
                || mr.getStatus() == MaterialRequisitionStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel MR in status " + mr.getStatus());
        }
        if (mr.getStatus() == MaterialRequisitionStatus.CANCELLED) {
            return toResponse(mr);
        }
        mr.setStatus(MaterialRequisitionStatus.CANCELLED);
        mr.setUpdatedAt(Instant.now());
        return toResponse(mrRepository.save(mr));
    }

    @Transactional(readOnly = true)
    public MaterialRequisitionResponse get(Long id) {
        MaterialRequisition mr = getOrThrow(id);
        return toResponse(mr, findLinkedPo(mr.getId()), true);
    }

    @Transactional(readOnly = true)
    public Page<MaterialRequisitionResponse> list(Pageable pageable) {
        Page<MaterialRequisition> mrPage = mrRepository.findAll(pageable);
        Set<Long> mrIds = mrPage.getContent().stream()
                .map(MaterialRequisition::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, PurchaseOrder> poByMrId = mrIds.isEmpty()
                ? Collections.emptyMap()
                : poRepository.findByMrIdIn(mrIds).stream()
                .filter(po -> po.getMrId() != null)
                .collect(Collectors.toMap(
                        PurchaseOrder::getMrId,
                        Function.identity(),
                        (left, right) -> left
                ));

        return mrPage.map(mr -> toResponse(mr, poByMrId.get(mr.getId()), false));
    }

    // Helpers

    private void validateLines(List<MaterialRequisitionLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one MR line is required");
        }
        Set<Long> itemIds = lines.stream().map(MaterialRequisitionLineRequest::getItemId).collect(Collectors.toSet());
        Map<Long, InventoryItem> itemsById = inventoryItemRepository.findAllById(itemIds).stream()
                .filter(item -> !item.isDeleted())
                .collect(Collectors.toMap(InventoryItem::getId, Function.identity()));

        for (MaterialRequisitionLineRequest line : lines) {
            if (line.getRequestedQty() == null || line.getRequestedQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestedQty must be greater than zero");
            }
            InventoryItem item = itemsById.get(line.getItemId());
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory item not found: " + line.getItemId());
            }
            if (!item.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory item is inactive: " + line.getItemId());
            }
        }
    }

    private MaterialRequisitionLine buildLine(MaterialRequisition mr, MaterialRequisitionLineRequest request) {
        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory item not found: " + request.getItemId()));

        BigDecimal qty = request.getRequestedQty().setScale(4, RoundingMode.HALF_UP);

        String uom = trim(request.getUom());
        if (uom == null && item.getUnitOfMeasure() != null) {
            uom = item.getUnitOfMeasure().name();
        }

        return MaterialRequisitionLine.builder()
                .materialRequisition(mr)
                .itemId(item.getId())
                .requestedQty(qty)
                .uom(uom)
                .remarks(trim(request.getRemarks()))
                .build();
    }

    private MaterialRequisition getOrThrow(Long id) {
        return mrRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material Requisition not found"));
    }

    private MaterialRequisitionResponse toResponse(MaterialRequisition mr) {
        return toResponse(mr, null, false);
    }

    private MaterialRequisitionResponse toResponse(MaterialRequisition mr,
                                                   PurchaseOrder linkedPo,
                                                   boolean includeLinkedGrns) {
        List<MaterialRequisitionLine> mrLines = Optional.ofNullable(mr.getLines())
                .orElseGet(Collections::emptyList);

        Set<Long> itemIds = mrLines.stream()
                .map(MaterialRequisitionLine::getItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<Long, BigDecimal> costPerUnitByItemId = itemIds.isEmpty()
                ? Collections.emptyMap()
                : inventoryItemRepository.findAllById(itemIds).stream()
                .filter(item -> !item.isDeleted())
                .collect(Collectors.toMap(InventoryItem::getId, InventoryItem::getCostPerUnit));

        List<MaterialRequisitionLineResponse> lines = mrLines.stream()
                .map(line -> MaterialRequisitionLineResponse.builder()
                        .id(line.getId())
                        .itemId(line.getItemId())
                        .requestedQty(line.getRequestedQty())
                        .costPerUnit(costPerUnitByItemId.get(line.getItemId()))
                        .uom(line.getUom())
                        .remarks(line.getRemarks())
                .build())
                .toList();

        Long linkedPoId = null;
        String linkedPoNumber = null;
        List<MaterialRequisitionLinkedGrnResponse> linkedGrns = Collections.emptyList();

        if (linkedPo != null) {
            linkedPoId = linkedPo.getId();
            linkedPoNumber = linkedPo.getPoNumber();
        }
        if (includeLinkedGrns && linkedPo != null) {
            linkedGrns = grnRepository.findByPoId(linkedPo.getId()).stream()
                    .map(grn -> MaterialRequisitionLinkedGrnResponse.builder()
                            .id(grn.getId())
                            .grnNumber(grn.getGrnNumber())
                            .build())
                    .toList();
        }

        return MaterialRequisitionResponse.builder()
                .id(mr.getId())
                .mrNumber(mr.getMrNumber())
                .requestedByUserId(mr.getRequestedByUserId())
                .status(mr.getStatus())
                .neededByDate(mr.getNeededByDate())
                .notes(mr.getNotes())
                .department(mr.getDepartment())
                .shipToType(mr.getShipToType() != null ? mr.getShipToType().name() : null)
                .shipToWarehouseId(mr.getShipToWarehouseId())
                .shipToWorkOrderId(mr.getShipToWorkOrderId())
                .approvedByUserId(mr.getApprovedByUserId())
                .approvedAt(mr.getApprovedAt())
                .rejectedByUserId(mr.getRejectedByUserId())
                .rejectedAt(mr.getRejectedAt())
                .rejectionReason(mr.getRejectionReason())
                .createdAt(mr.getCreatedAt())
                .updatedAt(mr.getUpdatedAt())
                .poId(linkedPoId)
                .poNumber(linkedPoNumber)
                .linkedGrns(linkedGrns)
                .lines(lines)
                .build();
    }

    private PurchaseOrder findLinkedPo(Long mrId) {
        if (mrId == null) return null;
        return poRepository.findByMrId(mrId).stream().findFirst().orElse(null);
    }

    private String trim(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private String requireText(String value, String message) {
        String trimmed = trim(value);
        if (trimmed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return trimmed;
    }

    private ShippingTarget resolveShippingTarget(String shipToTypeRaw, Long warehouseId, Long workOrderId) {
        if (shipToTypeRaw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shipToType is required (WAREHOUSE or WORK_SITE)");
        }
        MaterialRequisitionShipToType type;
        try {
            type = MaterialRequisitionShipToType.valueOf(shipToTypeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipToType. Allowed: WAREHOUSE, WORK_SITE");
        }

        Long resolvedWarehouseId = null;
        Long resolvedWorkOrderId = null;

        if (type == MaterialRequisitionShipToType.WAREHOUSE) {
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

    private record ShippingTarget(MaterialRequisitionShipToType type, Long warehouseId, Long workOrderId) { }
}
