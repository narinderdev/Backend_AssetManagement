package com.example.eam.InventoryManagement.Dto;

import com.example.eam.Enum.UnitOfMeasure;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryItemCreateRequest {

    @Size(max = 64)
    private String itemId;

    @Size(max = 128)
    private String skuNumber;

    @NotBlank
    private String itemName;

    @NotBlank
    @Size(max = 128)
    private String category;

    @NotNull
    private UnitOfMeasure unitOfMeasure;

    private String manufacturer;
    private String manufacturerPartNumber;

    @NotNull @Min(0)
    private Integer stockLevel;

    @NotNull @Min(0)
    private Integer reorderPoint;

    @NotNull @Min(1)
    private Integer reorderQuantity;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal costPerUnit;

    @Min(0)
    private Integer minStockLevel;

    @Min(0)
    private Integer maxStockLevel;

    // Vendor lookup
    private Long primaryVendorDbId;

    // Warehouse lookup
    private Long warehouseId;

    // Accounting
    private String glAccountString;
    private String expenseCode;

    // optional
    private Boolean active;
}

