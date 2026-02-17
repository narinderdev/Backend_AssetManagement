package com.example.eam.InventoryManagement.Dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryItemReportRow {
    private Long id;
    private String itemId;
    private String skuNumber;
    private String itemName;
    private Integer stockLevel;
    private Integer maxStockLevel;
    private Integer minStockLevel;
    private Integer reorderPoint;
    private String warehouseName;
    private Long warehouseId;
    private BigDecimal unitCost;
}
