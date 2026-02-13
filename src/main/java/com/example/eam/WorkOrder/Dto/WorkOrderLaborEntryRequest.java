package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkOrderLaborEntryRequest {

    @NotNull(message = "technicianId is required")
    private Long technicianId;

    @NotNull(message = "hourlyRate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "hourlyRate must be greater than zero")
    private BigDecimal hourlyRate;
}
