package com.example.eam.InventoryManagement.Dto;

import com.example.eam.Enum.UnitOfMeasure;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonPropertyOrder({
        "id","itemId","skuNumber","itemName","category","unitOfMeasure",
        "manufacturer","manufacturerPartNumber",
        "stockLevel","reorderPoint","reorderQuantity",
        "minStockLevel","maxStockLevel","costPerUnit",
        "primaryVendorDbId","primaryVendorName",
        "warehouseId","warehouseName","zoneAisle","rackShelf","binCode","binDescription",
        "active","createdAt","updatedAt"
})
public class InventoryItemResponse {
    private Long id;
    private String itemId;
    private String skuNumber;
    private String itemName;
    private String category;
    private UnitOfMeasure unitOfMeasure;

    private String manufacturer;
    private String manufacturerPartNumber;

    private Integer stockLevel;
    private Integer reorderPoint;
    private Integer reorderQuantity;

    private Integer minStockLevel;
    private Integer maxStockLevel;

    private BigDecimal costPerUnit;

    private Long primaryVendorDbId;
    private String primaryVendorName;

    private Long warehouseId;
    private String warehouseName;
    private String zoneAisle;
    private String rackShelf;
    private String binCode;
    private String binDescription;

    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

