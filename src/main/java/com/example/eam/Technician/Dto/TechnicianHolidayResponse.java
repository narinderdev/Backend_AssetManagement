package com.example.eam.Technician.Dto;

import com.example.eam.Enum.TechnicianHolidayType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class TechnicianHolidayResponse {
    Long id;
    String holidayName;
    TechnicianHolidayType holidayType;
    LocalDate holidayDate;
    String notes;
    LocalDateTime createdAt;
}
