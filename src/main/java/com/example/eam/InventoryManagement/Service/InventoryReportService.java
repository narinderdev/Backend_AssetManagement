package com.example.eam.InventoryManagement.Service;

import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.Enum.InventoryTransactionType;
import com.example.eam.InventoryManagement.Dto.*;
import com.example.eam.InventoryManagement.Entity.InventoryAuditLog;
import com.example.eam.InventoryManagement.Entity.InventoryItem;
import com.example.eam.InventoryManagement.Repository.InventoryAuditLogRepository;
import com.example.eam.InventoryManagement.Repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private final InventoryItemRepository itemRepository;
    private final InventoryAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public InventoryReportResponse getReport(InventoryReportView view,
                                             Long warehouseId,
                                             boolean lowStockOnly,
                                             InventoryTransactionType txnType,
                                             InventoryReferenceType refType,
                                             String period,
                                             Pageable pageable,
                                             int topN) {
        InventoryReportResponse.InventoryReportResponseBuilder builder = InventoryReportResponse.builder();

        if (view == InventoryReportView.ITEMS) {
            List<InventoryItem> items = itemRepository.findAll().stream()
                    .filter(i -> !Boolean.TRUE.equals(i.isDeleted()))
                    .filter(i -> warehouseId == null || (i.getWarehouse() != null && warehouseId.equals(i.getWarehouse().getId())))
                    .filter(i -> !lowStockOnly || isLowStock(i))
                    .toList();

            List<InventoryItemReportRow> itemRows = items.stream()
                    .map(this::toItemRow)
                    .toList();

            List<InventoryItemReportRow> topHighStock = items.stream()
                    .sorted(Comparator.comparingInt(i -> -safeInt(((InventoryItem) i).getStockLevel())))
                    .limit(5)
                    .map(this::toItemRow)
                    .toList();

            List<InventoryStockValuePoint> topValue = items.stream()
                    .map(i -> InventoryStockValuePoint.builder()
                            .itemName(i.getItemName())
                            .itemId(i.getId())
                            .skuNumber(i.getSkuNumber())
                            .stockValue(stockValue(i))
                            .build())
                    .sorted(Comparator.comparing(InventoryStockValuePoint::getStockValue, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                    .limit(Math.max(topN, 5))
                    .toList();

            builder.items(itemRows)
                    .top5HighStock(topHighStock)
                    .topStockValue(topValue);
        } else if (view == InventoryReportView.TRANSACTIONS) {
            LocalDateTimeRange range = resolveRange(period);
            Page<InventoryAuditLog> page = auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                    range.from(), range.to(), pageable);

            List<InventoryTransactionReportRow> rows = page.getContent().stream()
                    .filter(log -> txnType == null || log.getTransactionType() == txnType)
                    .filter(log -> refType == null || log.getReferenceType() == refType)
                    .filter(log -> warehouseId == null || (log.getInventoryItem() != null
                            && log.getInventoryItem().getWarehouse() != null
                            && warehouseId.equals(log.getInventoryItem().getWarehouse().getId())))
                    .map(this::toTxnRow)
                    .toList();

            Map<InventoryTransactionType, Integer> totals = new EnumMap<>(InventoryTransactionType.class);
            for (InventoryTransactionType t : InventoryTransactionType.values()) totals.put(t, 0);
            for (InventoryTransactionReportRow row : rows) {
                totals.put(row.getTransactionType(),
                        totals.get(row.getTransactionType()) + (row.getQtyChange() != null ? row.getQtyChange() : 0));
            }

            builder.transactions(rows)
                    .totalQuantityByTxnType(totals);
        }

        return builder.build();
    }

    private InventoryItemReportRow toItemRow(InventoryItem i) {
        return InventoryItemReportRow.builder()
                .id(i.getId())
                .itemId(i.getItemId())
                .skuNumber(i.getSkuNumber())
                .itemName(i.getItemName())
                .stockLevel(safeInt(i.getStockLevel()))
                .maxStockLevel(i.getMaxStockLevel())
                .minStockLevel(i.getMinStockLevel())
                .reorderPoint(i.getReorderPoint())
                .warehouseId(i.getWarehouse() != null ? i.getWarehouse().getId() : null)
                .warehouseName(i.getWarehouse() != null ? i.getWarehouse().getName() : null)
                .unitCost(i.getCostPerUnit())
                .build();
    }

    private InventoryTransactionReportRow toTxnRow(InventoryAuditLog log) {
        InventoryItem item = log.getInventoryItem();
        return InventoryTransactionReportRow.builder()
                .id(log.getId())
                .dateTime(log.getCreatedAt())
                .transactionType(log.getTransactionType())
                .inventoryItemId(item != null ? item.getId() : null)
                .itemId(item != null ? item.getItemId() : null)
                .skuNumber(item != null ? item.getSkuNumber() : null)
                .itemName(item != null ? item.getItemName() : null)
                .qtyChange(log.getVarianceQuantity())
                .qtyBefore(log.getBeforeQuantity())
                .qtyAfter(log.getAfterQuantity())
                .referenceType(log.getReferenceType())
                .referenceNumber(log.getReferenceNumber())
                .performedBy(log.getPerformedBy())
                .reason(log.getReason())
                .warehouseId(item != null && item.getWarehouse() != null ? item.getWarehouse().getId() : null)
                .warehouseName(item != null && item.getWarehouse() != null ? item.getWarehouse().getName() : null)
                .build();
    }

    private boolean isLowStock(InventoryItem i) {
        int stock = safeInt(i.getStockLevel());
        Integer threshold = i.getReorderPoint() != null ? i.getReorderPoint() : i.getMinStockLevel();
        return threshold != null && stock <= threshold;
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private BigDecimal stockValue(InventoryItem i) {
        if (i.getCostPerUnit() == null) return BigDecimal.ZERO;
        return i.getCostPerUnit().multiply(BigDecimal.valueOf(safeInt(i.getStockLevel())));
    }

    private LocalDateTimeRange resolveRange(String period) {
        LocalDateTime end = LocalDateTime.now();
        if (period == null) {
            return new LocalDateTimeRange(end.minusYears(10), end);
        }
        return switch (period.trim().toUpperCase()) {
            case "THIS_MONTH" -> new LocalDateTimeRange(LocalDate.now().withDayOfMonth(1).atStartOfDay(), end);
            case "THIS_YEAR" -> new LocalDateTimeRange(LocalDate.now().withDayOfYear(1).atStartOfDay(), end);
            case "THIS_WEEK" -> {
                LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
                yield new LocalDateTimeRange(monday.atStartOfDay(), end);
            }
            default -> new LocalDateTimeRange(end.minusYears(10), end);
        };
    }

    private record LocalDateTimeRange(LocalDateTime from, LocalDateTime to) {}
}
