package com.example.eam.InventoryManagement.Dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryStockValuePoint {
    private String itemName;
    private Long itemId;
    private String skuNumber;
    private BigDecimal stockValue; // onHandQty * unitCost
}
