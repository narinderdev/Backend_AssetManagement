package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkOrderAvailabilityRequest {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    /**
     * How many hours are required for the work in a day.
     */
    @NotNull
    @Min(1)
    private Integer hoursRequired;

    private Long teamId;
    private Long technicianId;
}
