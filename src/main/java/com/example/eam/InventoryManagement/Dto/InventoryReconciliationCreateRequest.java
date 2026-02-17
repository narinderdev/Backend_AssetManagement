package com.example.eam.InventoryManagement.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryReconciliationCreateRequest {
    @NotNull
    private Long warehouseId;
    @NotNull
    private Long inventoryItemId;
    @NotNull
    private LocalDate reconcileDate;
    private String enteredBy;
    @NotNull
    private Integer physicalQuantity;
    private String reason;
}
