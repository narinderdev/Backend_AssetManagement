package com.example.eam.WorkOrder.Dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkOrderApproveRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "estimatedLaborHours must be greater than zero")
    private BigDecimal estimatedLaborHours;

    @DecimalMin(value = "0.0", inclusive = false, message = "estimatedMaterialCost must be greater than zero")
    private BigDecimal estimatedMaterialCost;

    private String approvalNotes;

    @NotBlank
    private String approvedBy;
}
