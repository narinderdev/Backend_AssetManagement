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
     * How many days are required within the range for availability (days API).
     */
    @Min(1)
    private Integer daysRequired;

    /**
     * How many hours are required for the work in a day (time-slots API).
     */
    @Min(1)
    private Integer hoursRequired;

    private Long teamId;
    private Long technicianId;
}
