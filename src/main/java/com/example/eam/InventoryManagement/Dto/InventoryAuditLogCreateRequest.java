package com.example.eam.InventoryManagement.Dto;

import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.Enum.InventoryTransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAuditLogCreateRequest {
    @NotNull
    private Long inventoryItemId;
    @NotNull
    private InventoryTransactionType transactionType;
    @NotNull
    private InventoryReferenceType referenceType;
    private String referenceNumber;
    @NotNull
    private Integer beforeQuantity;
    @NotNull
    private Integer afterQuantity;
    private String performedBy;
    private String reason;
}
