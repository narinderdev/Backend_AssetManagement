package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceTechnicianRateRequest {

    @NotNull
    private Long technicianId;

    /**
     * Hourly rate to use for the invoice (currency symbol handled separately).
     */
    @NotNull
    @DecimalMin(value = "0.00", message = "Rate cannot be negative")
    private BigDecimal hourlyRate;
}
