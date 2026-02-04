package com.example.eam.Technician.Dto;

import com.example.eam.Enum.TechnicianHolidayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TechnicianHolidayRequest {

    @NotBlank
    @Size(max = 255)
    private String holidayName;

    @NotNull
    private TechnicianHolidayType holidayType;

    @NotNull
    private LocalDate holidayDate;

    @Size(max = 512)
    private String notes;
}
