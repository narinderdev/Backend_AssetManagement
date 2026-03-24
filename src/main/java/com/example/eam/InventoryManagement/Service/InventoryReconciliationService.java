package com.example.eam.InventoryManagement.Service;

import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Enum.InventoryReconciliationStatus;
import com.example.eam.InventoryManagement.Dto.InventoryReconciliationCreateRequest;
import com.example.eam.InventoryManagement.Dto.InventoryReconciliationDecisionRequest;
import com.example.eam.InventoryManagement.Dto.InventoryReconciliationResponse;
import com.example.eam.InventoryManagement.Dto.InventoryReconciliationUpdateRequest;
import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Entity.InventoryReconciliation;
import com.example.eam.InventoryManagement.Entity.Warehouse;
import com.example.eam.InventoryManagement.Service.InventoryAuditLogService;
import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import com.example.eam.InventoryManagement.Repository.InventoryReconciliationRepository;
import com.example.eam.InventoryManagement.Repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryReconciliationService {

    private final InventoryReconciliationRepository reconciliationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryAuditLogService auditLogService;

    @Transactional
    public InventoryReconciliationResponse create(InventoryReconciliationCreateRequest req) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        Warehouse warehouse = warehouseRepository.findByIdAndDeletedFalseAndCompanyId(req.getWarehouseId(), companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalseAndCompanyId(req.getInventoryItemId(), companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));

        int systemQty = item.getStockLevel() != null ? item.getStockLevel() : 0;
        int physicalQty = req.getPhysicalQuantity();
        int varianceQty = physicalQty - systemQty;
        BigDecimal costSnap = item.getCostPerUnit();
        BigDecimal varianceCost = costSnap != null ? costSnap.multiply(BigDecimal.valueOf(varianceQty)) : null;

        InventoryReconciliationStatus status = varianceQty == 0
                ? InventoryReconciliationStatus.POSTED
                : InventoryReconciliationStatus.SUBMITTED;

        InventoryReconciliation entity = InventoryReconciliation.builder()
                .companyId(companyId)
                .warehouse(warehouse)
                .inventoryItem(item)
                .reconcileDate(req.getReconcileDate())
                .enteredBy(trim(req.getEnteredBy()))
                .systemQuantity(systemQty)
                .physicalQuantity(physicalQty)
                .varianceQuantity(varianceQty)
                .costPerUnitSnapshot(costSnap)
                .varianceCost(varianceCost)
                .status(status)
                .reason(trim(req.getReason()))
                .build();

        InventoryReconciliation saved = reconciliationRepository.save(entity);

        // If variance is zero and status is POSTED, log audit entry reflecting no change
        if (status == InventoryReconciliationStatus.POSTED) {
            auditLogService.recordAdjustment(
                    item,
                    systemQty,
                    physicalQty,
                    InventoryReferenceType.RECONCILIATION,
                    "REC-" + (saved.getId() != null ? saved.getId() : "pending"),
                    saved.getEnteredBy(),
                    saved.getReason()
            );
        }
        return toResponse(saved);
    }

    @Transactional
    public InventoryReconciliationResponse update(Long id, InventoryReconciliationUpdateRequest req) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        InventoryReconciliation rec = getOrThrow(id, companyId);
        if (rec.getStatus() != InventoryReconciliationStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUBMITTED records can be edited");
        }

        Warehouse warehouse = warehouseRepository.findByIdAndDeletedFalseAndCompanyId(req.getWarehouseId(), companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalseAndCompanyId(req.getInventoryItemId(), companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));

        int systemQty = item.getStockLevel() != null ? item.getStockLevel() : 0;
        int physicalQty = req.getPhysicalQuantity();
        int varianceQty = physicalQty - systemQty;
        BigDecimal costSnap = item.getCostPerUnit();
        BigDecimal varianceCost = costSnap != null ? costSnap.multiply(BigDecimal.valueOf(varianceQty)) : null;

        rec.setWarehouse(warehouse);
        rec.setInventoryItem(item);
        rec.setReconcileDate(req.getReconcileDate());
        rec.setEnteredBy(trim(req.getEnteredBy()));
        rec.setSystemQuantity(systemQty);
        rec.setPhysicalQuantity(physicalQty);
        rec.setVarianceQuantity(varianceQty);
        rec.setCostPerUnitSnapshot(costSnap);
        rec.setVarianceCost(varianceCost);
        rec.setReason(trim(req.getReason()));

        rec.setStatus(varianceQty == 0 ? InventoryReconciliationStatus.POSTED : InventoryReconciliationStatus.SUBMITTED);
        rec.setApprovedBy(null);
        rec.setApprovedAt(null);
        rec.setApprovalComment(null);
        rec.setRejectedBy(null);
        rec.setRejectedAt(null);
        rec.setRejectionComment(null);

        InventoryReconciliation saved = reconciliationRepository.save(rec);

        if (saved.getStatus() == InventoryReconciliationStatus.POSTED) {
            auditLogService.recordAdjustment(
                    item,
                    systemQty,
                    physicalQty,
                    InventoryReferenceType.RECONCILIATION,
                    "REC-" + saved.getId(),
                    saved.getEnteredBy(),
                    saved.getReason()
            );
        }

        return toResponse(saved);
    }

    @Transactional
    public InventoryReconciliationResponse approve(Long id, InventoryReconciliationDecisionRequest req) {
        InventoryReconciliation rec = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        if (rec.getStatus() != InventoryReconciliationStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUBMITTED records can be approved");
        }
        rec.setStatus(InventoryReconciliationStatus.APPROVED);
        rec.setApprovedBy(trim(req.getActor()));
        rec.setApprovedAt(LocalDateTime.now());
        rec.setApprovalComment(trim(req.getComment()));
        rec.setUpdatedAt(LocalDateTime.now());
        return toResponse(reconciliationRepository.save(rec));
    }

    @Transactional
    public InventoryReconciliationResponse reject(Long id, InventoryReconciliationDecisionRequest req) {
        InventoryReconciliation rec = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        if (rec.getStatus() != InventoryReconciliationStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUBMITTED records can be rejected");
        }
        rec.setStatus(InventoryReconciliationStatus.REJECTED);
        rec.setRejectedBy(trim(req.getActor()));
        rec.setRejectedAt(LocalDateTime.now());
        rec.setRejectionComment(trim(req.getComment()));
        rec.setUpdatedAt(LocalDateTime.now());
        return toResponse(reconciliationRepository.save(rec));
    }

    @Transactional
    public InventoryReconciliationResponse post(Long id) {
        InventoryReconciliation rec = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        if (rec.getStatus() == InventoryReconciliationStatus.POSTED) {
            return toResponse(rec);
        }
        if (rec.getStatus() == InventoryReconciliationStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approve before posting");
        }

        // status APPROVED -> POSTED
        InventoryItem item = rec.getInventoryItem();
        int before = item.getStockLevel() != null ? item.getStockLevel() : 0;
        int after = rec.getPhysicalQuantity();
        item.setStockLevel(after);
        inventoryItemRepository.save(item);

        rec.setStatus(InventoryReconciliationStatus.POSTED);
        rec.setUpdatedAt(LocalDateTime.now());
        InventoryReconciliation saved = reconciliationRepository.save(rec);

        auditLogService.recordAdjustment(
                item,
                before,
                after,
                InventoryReferenceType.RECONCILIATION,
                "REC-" + saved.getId(),
                saved.getApprovedBy() != null ? saved.getApprovedBy() : saved.getEnteredBy(),
                saved.getReason()
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventoryReconciliationResponse get(Long id) {
        return toResponse(getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null)));
    }

    @Transactional(readOnly = true)
    public Page<InventoryReconciliationResponse> list(Pageable pageable) {
        Long companyId = CompanyContextHolder.getCompanyId().orElse(null);
        return reconciliationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable).map(this::toResponse);
    }

    @Transactional
    public void delete(Long id) {
        InventoryReconciliation rec = getOrThrow(id, CompanyContextHolder.getCompanyId().orElse(null));
        reconciliationRepository.delete(rec);
    }

    private InventoryReconciliation getOrThrow(Long id, Long companyId) {
        return reconciliationRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory reconciliation not found"));
    }

    private InventoryReconciliationResponse toResponse(InventoryReconciliation rec) {
        return InventoryReconciliationResponse.builder()
                .id(rec.getId())
                .warehouseId(rec.getWarehouse() != null ? rec.getWarehouse().getId() : null)
                .warehouseName(rec.getWarehouse() != null ? rec.getWarehouse().getName() : null)
                .inventoryItemId(rec.getInventoryItem() != null ? rec.getInventoryItem().getId() : null)
                .itemId(rec.getInventoryItem() != null ? rec.getInventoryItem().getItemId() : null)
                .skuNumber(rec.getInventoryItem() != null ? rec.getInventoryItem().getSkuNumber() : null)
                .itemName(rec.getInventoryItem() != null ? rec.getInventoryItem().getItemName() : null)
                .reconcileDate(rec.getReconcileDate())
                .enteredBy(rec.getEnteredBy())
                .systemQuantity(rec.getSystemQuantity())
                .physicalQuantity(rec.getPhysicalQuantity())
                .varianceQuantity(rec.getVarianceQuantity())
                .costPerUnitSnapshot(rec.getCostPerUnitSnapshot())
                .varianceCost(rec.getVarianceCost())
                .reason(rec.getReason())
                .status(rec.getStatus())
                .approvedBy(rec.getApprovedBy())
                .approvedAt(rec.getApprovedAt())
                .approvalComment(rec.getApprovalComment())
                .rejectedBy(rec.getRejectedBy())
                .rejectedAt(rec.getRejectedAt())
                .rejectionComment(rec.getRejectionComment())
                .createdAt(rec.getCreatedAt())
                .updatedAt(rec.getUpdatedAt())
                .build();
    }

    private String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
