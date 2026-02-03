package com.example.eam.Technician.Dto;

import com.example.eam.Enum.TechnicianCalendarStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class DailyAvailabilityDto {
    LocalDate date;
    TechnicianCalendarStatus status;
}
