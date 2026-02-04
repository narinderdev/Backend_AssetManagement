package com.example.eam.Technician.Dto;

import com.example.eam.Enum.TechnicianHolidayType;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TechnicianHolidayPatchRequest {
    private String holidayName;
    private TechnicianHolidayType holidayType;
    private LocalDate holidayDate;
    @Size(max = 512)
    private String notes;
}
