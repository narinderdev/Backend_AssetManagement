package com.example.eam.InventoryManagement.Dto;

import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.Enum.InventoryTransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryAuditLogResponse {
    private Long id;
    private InventoryTransactionType transactionType;
    private InventoryReferenceType referenceType;
    private String referenceNumber;
    private Long inventoryItemId;
    private String itemId;
    private String skuNumber;
    private String itemName;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private Integer varianceQuantity;
    private String performedBy;
    private String reason;
    private LocalDateTime createdAt;
}
