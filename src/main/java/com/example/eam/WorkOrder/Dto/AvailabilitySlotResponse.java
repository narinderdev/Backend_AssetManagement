package com.example.eam.WorkOrder.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AvailabilitySlotResponse {
    LocalDateTime start;
    LocalDateTime end;

    Long technicianId;
    String technicianName;

    Long teamId;
    String teamName;
}
