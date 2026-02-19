package com.example.eam.WorkOrder.Dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class WorkOrderBudgetSummaryResponse {
    private List<WorkOrderBudgetRowResponse> workOrders;
    private BigDecimal totalEstimatedBudget;
    private BigDecimal totalActualBudget;
    private BigDecimal totalVarianceAmount;
    private BigDecimal totalVariancePercentage;
    private LocalDate startDate;
    private LocalDate endDate;
}
