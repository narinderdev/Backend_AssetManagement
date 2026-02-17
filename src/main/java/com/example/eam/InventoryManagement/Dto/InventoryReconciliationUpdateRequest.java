package com.example.eam.InventoryManagement.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryReconciliationUpdateRequest {
    @NotNull
    private Long warehouseId;
    @NotNull
    private Long inventoryItemId;
    @NotNull
    private LocalDate reconcileDate;
    @NotNull
    private Integer physicalQuantity;
    private String enteredBy;
    private String reason;
}
