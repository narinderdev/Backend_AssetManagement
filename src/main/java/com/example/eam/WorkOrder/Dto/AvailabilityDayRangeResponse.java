package com.example.eam.WorkOrder.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class AvailabilityDayRangeResponse {
    LocalDate startDate;
    LocalDate endDate;

    Long technicianId;
    String technicianName;

    Long teamId;
    String teamName;
}
