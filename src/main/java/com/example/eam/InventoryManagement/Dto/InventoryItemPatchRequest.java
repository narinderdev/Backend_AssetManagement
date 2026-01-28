package com.example.eam.InventoryManagement.Dto;

import com.example.eam.Enum.UnitOfMeasure;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryItemPatchRequest {

    private String itemName;
    private String category;
    private UnitOfMeasure unitOfMeasure;

    private String manufacturer;
    private String manufacturerPartNumber;

    private String skuNumber;

    private Integer stockLevel;
    private Integer reorderPoint;
    private Integer reorderQuantity;

    private BigDecimal costPerUnit;

    private Integer minStockLevel;
    private Integer maxStockLevel;

    private Long primaryVendorDbId;
    private Long warehouseId;
    private Boolean active;

    private String glAccountString;
    private String expenseCode;
}

