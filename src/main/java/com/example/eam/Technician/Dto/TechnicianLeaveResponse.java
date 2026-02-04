package com.example.eam.Technician.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class TechnicianLeaveResponse {
    Long id;
    Long technicianId;
    String technicianName;
    LocalDate startDate;
    LocalDate endDate;
    String reason;
    LocalDateTime createdAt;
}
