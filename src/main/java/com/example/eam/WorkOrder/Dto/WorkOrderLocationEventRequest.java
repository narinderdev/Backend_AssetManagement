package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WorkOrderLocationEventRequest {

    @NotNull
    private Long technicianId;

    private Long teamId;

    @NotNull
    @DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90")
    @DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90")
    private BigDecimal latitude;

    @NotNull
    @DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180")
    @DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180")
    private BigDecimal longitude;

    private LocalDateTime observedAt;
}
