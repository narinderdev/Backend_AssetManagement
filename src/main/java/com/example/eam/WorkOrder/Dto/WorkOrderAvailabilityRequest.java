package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkOrderAvailabilityRequest {

    /**
     * Start of the search window (inclusive). Defaults to today if not provided.
     */
    private LocalDate fromDate;

    /**
     * End of the search window (inclusive). Defaults to fromDate + 14 days if not provided.
     */
    private LocalDate toDate;

    /**
     * Slot granularity in minutes. Defaults to 60 if not provided.
     */
    @Min(1)
    private Integer slotMinutes;
}
