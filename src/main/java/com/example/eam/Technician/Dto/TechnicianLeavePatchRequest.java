package com.example.eam.Technician.Dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TechnicianLeavePatchRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    @Size(max = 512)
    private String reason;
}
