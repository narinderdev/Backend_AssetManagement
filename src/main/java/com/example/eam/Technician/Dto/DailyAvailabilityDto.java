package com.example.eam.Technician.Dto;

import com.example.eam.Enum.TechnicianCalendarStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class DailyAvailabilityDto {
    LocalDate date;
    TechnicianCalendarStatus status;
    List<TimeWindowDto> busyWindows;
    List<TimeWindowDto> freeWindows;
}
