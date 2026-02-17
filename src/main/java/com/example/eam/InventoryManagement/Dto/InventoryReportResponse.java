package com.example.eam.InventoryManagement.Dto;

import com.example.eam.Enum.InventoryTransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InventoryReportResponse {
    // Items view
    private List<InventoryItemReportRow> items;
    private List<InventoryItemReportRow> top5HighStock;
    private List<InventoryStockValuePoint> topStockValue;

    // Transactions view
    private List<InventoryTransactionReportRow> transactions;
    private Map<InventoryTransactionType, Integer> totalQuantityByTxnType;
}
