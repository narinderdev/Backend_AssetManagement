package com.example.eam.WorkOrder.Service;

import com.example.eam.WorkOrder.Dto.WorkOrderBudgetRowResponse;
import com.example.eam.WorkOrder.Dto.WorkOrderBudgetSummaryResponse;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Repository.WorkOrderLaborEntryRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderMaterialPlanRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderMaterialUsageRepository;
import com.example.eam.WorkOrder.Repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkOrderBudgetService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderMaterialPlanRepository workOrderMaterialPlanRepository;
    private final WorkOrderMaterialUsageRepository workOrderMaterialUsageRepository;
    private final WorkOrderLaborEntryRepository workOrderLaborEntryRepository;

    @Transactional(readOnly = true)
    public WorkOrderBudgetSummaryResponse getBudgetSummary(
            String assetId,
            String workOrderId,
            String period
    ) {
        Long assetDbId = parseLongOrNull(assetId);
        Long workOrderDbId = parseLongOrNull(workOrderId);
        DateRange range = resolveDateRange(period);
        List<WorkOrder> workOrders = workOrderRepository.findForBudgetReport(
                assetId,
                assetDbId,
                workOrderId,
                workOrderDbId,
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        if (workOrders.isEmpty()) {
            return WorkOrderBudgetSummaryResponse.builder()
                    .workOrders(List.of())
                    .totalEstimatedBudget(BigDecimal.ZERO)
                    .totalActualBudget(BigDecimal.ZERO)
                    .totalVarianceAmount(BigDecimal.ZERO)
                    .totalVariancePercentage(BigDecimal.ZERO)
                    .startDate(range.startDate())
                    .endDate(range.endDate())
                    .build();
        }

        List<Long> workOrderIds = workOrders.stream()
                .map(WorkOrder::getId)
                .toList();

        Map<Long, BigDecimal> plannedMaterialCosts = mapCostRows(
                workOrderMaterialPlanRepository.sumPlannedCostByWorkOrderIds(workOrderIds)
        );
        Map<Long, BigDecimal> actualMaterialCosts = mapCostRows(
                workOrderMaterialUsageRepository.sumActualCostByWorkOrderIds(workOrderIds)
        );
        Map<Long, LaborAggregate> laborAggregates = mapLaborRows(
                workOrderLaborEntryRepository.sumLaborByWorkOrderIds(workOrderIds)
        );

        List<WorkOrderBudgetRowResponse> rows = new ArrayList<>(workOrders.size());
        BigDecimal totalEstimated = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;

        for (WorkOrder wo : workOrders) {
            Long id = wo.getId();
            LaborAggregate labor = laborAggregates.get(id);

            BigDecimal estimatedMaterialCost = firstNonNull(
                    wo.getEstimatedMaterialCost(),
                    plannedMaterialCosts.get(id)
            );

            BigDecimal estimatedLaborCost = computeEstimatedLaborCost(
                    wo,
                    labor,
                    estimatedMaterialCost
            );

            BigDecimal estimatedBudget = add(estimatedMaterialCost, estimatedLaborCost);

            BigDecimal actualLaborCost = firstNonNull(
                    wo.getActualLaborCost(),
                    labor != null ? labor.totalCost() : null
            );
            BigDecimal actualLaborHours = firstNonNull(
                    wo.getActualLaborHours(),
                    labor != null ? labor.totalHours() : null
            );

            BigDecimal actualMaterialCost = firstNonNull(
                    wo.getActualMaterialCost(),
                    actualMaterialCosts.get(id)
            );

            BigDecimal actualBudget = add(actualLaborCost, actualMaterialCost);

            BigDecimal varianceAmount = computeVariance(actualBudget, estimatedBudget);
            BigDecimal variancePct = computeVariancePercentage(actualBudget, estimatedBudget);

            if (estimatedBudget != null) {
                totalEstimated = totalEstimated.add(estimatedBudget);
            }
            if (actualBudget != null) {
                totalActual = totalActual.add(actualBudget);
            }

            rows.add(WorkOrderBudgetRowResponse.builder()
                    .id(id)
                    .workOrderId(wo.getWorkOrderId())
                    .workOrderNumber(wo.getWoNumber())
                    .title(wo.getWoTitle())
                    .status(wo.getStatus())
                    .assetDbId(wo.getAsset() != null ? wo.getAsset().getId() : null)
                    .assetId(wo.getAsset() != null ? wo.getAsset().getAssetId() : null)
                    .assetName(wo.getAsset() != null ? wo.getAsset().getAssetName() : wo.getAssetNameInput())
                    .estimatedLaborHours(wo.getEstimatedLaborHours())
                    .estimatedLaborCost(estimatedLaborCost)
                    .estimatedMaterialCost(estimatedMaterialCost)
                    .estimatedBudget(estimatedBudget)
                    .actualLaborHours(actualLaborHours)
                    .actualLaborCost(actualLaborCost)
                    .actualMaterialCost(actualMaterialCost)
                    .actualBudget(actualBudget)
                    .varianceAmount(varianceAmount)
                    .variancePercentage(variancePct)
                    .build());
        }

        BigDecimal totalVariance = totalActual.subtract(totalEstimated);
        BigDecimal totalVariancePct = totalEstimated.compareTo(BigDecimal.ZERO) == 0
                ? null
                : totalVariance
                .divide(totalEstimated, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        return WorkOrderBudgetSummaryResponse.builder()
                .workOrders(rows)
                .totalEstimatedBudget(totalEstimated)
                .totalActualBudget(totalActual)
                .totalVarianceAmount(totalVariance)
                .totalVariancePercentage(totalVariancePct)
                .startDate(range.startDate())
                .endDate(range.endDate())
                .build();
    }

    private BigDecimal computeEstimatedLaborCost(WorkOrder wo, LaborAggregate labor, BigDecimal estimatedMaterialCost) {
        BigDecimal estimatedTotal = wo.getEstimatedTotalCost();
        if (estimatedTotal != null) {
            if (estimatedMaterialCost != null) {
                BigDecimal laborCost = estimatedTotal.subtract(estimatedMaterialCost);
                if (laborCost.compareTo(BigDecimal.ZERO) < 0) {
                    laborCost = BigDecimal.ZERO;
                }
                return laborCost.setScale(2, RoundingMode.HALF_UP);
            }
            return estimatedTotal.setScale(2, RoundingMode.HALF_UP);
        }

        if (wo.getEstimatedLaborHours() != null) {
            BigDecimal hourlyRate = labor != null && labor.avgHourlyRate().compareTo(BigDecimal.ZERO) > 0
                    ? labor.avgHourlyRate()
                    : null;
            if (hourlyRate != null) {
                return wo.getEstimatedLaborHours()
                        .multiply(hourlyRate)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        return null;
    }

    private BigDecimal computeVariance(BigDecimal actual, BigDecimal estimated) {
        if (actual == null || estimated == null) return null;
        return actual.subtract(estimated).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeVariancePercentage(BigDecimal actual, BigDecimal estimated) {
        if (actual == null || estimated == null) return null;
        if (estimated.compareTo(BigDecimal.ZERO) == 0) return null;
        return actual.subtract(estimated)
                .divide(estimated, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, BigDecimal> mapCostRows(List<Object[]> rows) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (rows == null) return result;
        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            Long id = ((Number) row[0]).longValue();
            result.put(id, toBigDecimal(row[1]));
        }
        return result;
    }

    private Map<Long, LaborAggregate> mapLaborRows(List<Object[]> rows) {
        Map<Long, LaborAggregate> result = new HashMap<>();
        if (rows == null) return result;
        for (Object[] row : rows) {
            if (row == null || row.length < 4) continue;
            Long id = ((Number) row[0]).longValue();
            BigDecimal totalCost = toBigDecimal(row[1]);
            BigDecimal totalHours = toBigDecimal(row[2]);
            BigDecimal avgRate = toBigDecimal(row[3]);
            result.put(id, new LaborAggregate(totalCost, totalHours, avgRate));
        }
        return result;
    }

    private BigDecimal firstNonNull(BigDecimal primary, BigDecimal fallback) {
        return primary != null ? primary : fallback;
    }

    private BigDecimal add(BigDecimal left, BigDecimal right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.add(right);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private Long parseLongOrNull(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private DateRange resolveDateRange(String period) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedStart;
        LocalDate resolvedEnd;
        String normalized = period != null ? period.trim().toUpperCase() : "THIS_MONTH";

        switch (normalized) {
            case "THIS_WEEK" -> {
                resolvedStart = today.with(DayOfWeek.MONDAY);
                resolvedEnd = resolvedStart.plusDays(6);
            }
            case "THIS_YEAR" -> {
                resolvedStart = today.withDayOfYear(1);
                resolvedEnd = today.withDayOfYear(today.lengthOfYear());
            }
            case "THIS_MONTH" -> {
                resolvedStart = today.withDayOfMonth(1);
                resolvedEnd = resolvedStart.withDayOfMonth(resolvedStart.lengthOfMonth());
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "period must be one of THIS_WEEK, THIS_MONTH, THIS_YEAR"
            );
        }

        LocalDateTime startDateTime = resolvedStart.atStartOfDay();
        LocalDateTime endDateTimeExclusive = resolvedEnd.plusDays(1).atStartOfDay();

        return new DateRange(resolvedStart, resolvedEnd, startDateTime, endDateTimeExclusive);
    }

    private record LaborAggregate(BigDecimal totalCost, BigDecimal totalHours, BigDecimal avgHourlyRate) { }

    private record DateRange(LocalDate startDate,
                             LocalDate endDate,
                             LocalDateTime startDateTime,
                             LocalDateTime endDateTimeExclusive) { }
}
