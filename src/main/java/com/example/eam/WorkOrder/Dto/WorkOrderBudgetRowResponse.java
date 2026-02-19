package com.example.eam.WorkOrder.Dto;

import com.example.eam.Enum.WorkOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WorkOrderBudgetRowResponse {
    private Long id;
    private String workOrderId;
    private String workOrderNumber;
    private String title;
    private WorkOrderStatus status;

    private Long assetDbId;
    private String assetId;
    private String assetName;

    private BigDecimal estimatedLaborHours;
    private BigDecimal estimatedLaborCost;
    private BigDecimal estimatedMaterialCost;
    private BigDecimal estimatedBudget;

    private BigDecimal actualLaborHours;
    private BigDecimal actualLaborCost;
    private BigDecimal actualMaterialCost;
    private BigDecimal actualBudget;

    private BigDecimal varianceAmount;
    private BigDecimal variancePercentage;
}
