package com.example.eam.InventoryManagement.Dto;

import com.example.eam.Enum.InventoryReconciliationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InventoryReconciliationResponse {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long inventoryItemId;
    private String itemId;
    private String skuNumber;
    private String itemName;
    private LocalDate reconcileDate;
    private String enteredBy;
    private Integer systemQuantity;
    private Integer physicalQuantity;
    private Integer varianceQuantity;
    private BigDecimal costPerUnitSnapshot;
    private BigDecimal varianceCost;
    private String reason;
    private InventoryReconciliationStatus status;
    private String approvedBy;
    private java.time.LocalDateTime approvedAt;
    private String approvalComment;
    private String rejectedBy;
    private java.time.LocalDateTime rejectedAt;
    private String rejectionComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
