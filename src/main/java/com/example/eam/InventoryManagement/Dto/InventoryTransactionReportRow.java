package com.example.eam.InventoryManagement.Dto;

import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.Enum.InventoryTransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryTransactionReportRow {
    private Long id;
    private LocalDateTime dateTime;
    private InventoryTransactionType transactionType;
    private Long inventoryItemId;
    private String itemId;
    private String skuNumber;
    private String itemName;
    private Integer qtyChange;
    private Integer qtyBefore;
    private Integer qtyAfter;
    private InventoryReferenceType referenceType;
    private String referenceNumber;
    private String performedBy;
    private String reason;
    private Long warehouseId;
    private String warehouseName;
}
