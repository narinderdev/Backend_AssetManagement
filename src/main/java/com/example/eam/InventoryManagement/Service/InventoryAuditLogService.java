package com.example.eam.InventoryManagement.Service;

import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.Enum.InventoryTransactionType;
import com.example.eam.InventoryManagement.Dto.InventoryAuditLogResponse;
import com.example.eam.InventoryManagement.Entity.InventoryAuditLog;
import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Repository.InventoryAuditLogRepository;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InventoryAuditLogService {

    private final InventoryAuditLogRepository repository;
    private final InventoryItemRepository itemRepository;

    @Transactional(readOnly = true)
    public InventoryAuditLogResponse get(Long id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log not found")));
    }

    @Transactional(readOnly = true)
    public Page<InventoryAuditLogResponse> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InventoryAuditLogResponse> listByItem(Long itemId, Pageable pageable) {
        return repository.findByInventoryItem_IdOrderByCreatedAtDesc(itemId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InventoryAuditLogResponse> searchBySku(String sku, Pageable pageable) {
        String normalizedSku = trim(sku);
        if (normalizedSku == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sku is required");
        }
        return repository.findByInventoryItem_SkuNumberContainingIgnoreCaseOrderByCreatedAtDesc(normalizedSku, pageable)
                .map(this::toResponse);
    }

    private InventoryAuditLogResponse toResponse(InventoryAuditLog log) {
        InventoryItem item = log.getInventoryItem();
        return InventoryAuditLogResponse.builder()
                .id(log.getId())
                .transactionType(log.getTransactionType())
                .referenceType(log.getReferenceType())
                .referenceNumber(log.getReferenceNumber())
                .inventoryItemId(item != null ? item.getId() : null)
                .itemId(item != null ? item.getItemId() : null)
                .skuNumber(item != null ? item.getSkuNumber() : null)
                .itemName(item != null ? item.getItemName() : null)
                .beforeQuantity(log.getBeforeQuantity())
                .afterQuantity(log.getAfterQuantity())
                .varianceQuantity(log.getVarianceQuantity())
                .performedBy(log.getPerformedBy())
                .reason(log.getReason())
                .createdAt(log.getCreatedAt())
                .build();
    }

    public InventoryAuditLog recordAdjustment(InventoryItem item,
                                              int beforeQty,
                                              int afterQty,
                                              InventoryReferenceType refType,
                                              String referenceNumber,
                                              String performedBy,
                                              String reason) {
        return recordTransaction(item, InventoryTransactionType.ADJUSTMENT, refType, referenceNumber, beforeQty, afterQty, performedBy, reason);
    }

    public InventoryAuditLog recordIssueForWorkOrder(InventoryItem item,
                                                     int beforeQty,
                                                     int afterQty,
                                                     String workOrderNumber,
                                                     String performedBy,
                                                     String reason) {
        return recordTransaction(item, InventoryTransactionType.ISSUE, InventoryReferenceType.WORK_ORDER, workOrderNumber, beforeQty, afterQty, performedBy, reason);
    }

    public InventoryAuditLog recordReceiveForPoOrGrn(InventoryItem item,
                                                     int beforeQty,
                                                     int afterQty,
                                                     InventoryReferenceType refType,
                                                     String referenceNumber,
                                                     String performedBy,
                                                     String reason) {
        return recordTransaction(item, InventoryTransactionType.RECEIVE, refType, referenceNumber, beforeQty, afterQty, performedBy, reason);
    }

    public InventoryAuditLog recordReturn(InventoryItem item,
                                          int beforeQty,
                                          int afterQty,
                                          InventoryReferenceType refType,
                                          String referenceNumber,
                                          String performedBy,
                                          String reason) {
        return recordTransaction(item, InventoryTransactionType.RETURN, refType, referenceNumber, beforeQty, afterQty, performedBy, reason);
    }

    private InventoryAuditLog recordTransaction(InventoryItem item,
                                                InventoryTransactionType type,
                                                InventoryReferenceType refType,
                                                String referenceNumber,
                                                int beforeQty,
                                                int afterQty,
                                                String performedBy,
                                                String reason) {
        InventoryAuditLog log = InventoryAuditLog.builder()
                .inventoryItem(item)
                .transactionType(type)
                .referenceType(refType != null ? refType : InventoryReferenceType.OTHER)
                .referenceNumber(trim(referenceNumber))
                .beforeQuantity(beforeQty)
                .afterQuantity(afterQty)
                .varianceQuantity(afterQty - beforeQty)
                .performedBy(trim(performedBy))
                .reason(trim(reason))
                .build();
        return repository.save(log);
    }

    private String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
