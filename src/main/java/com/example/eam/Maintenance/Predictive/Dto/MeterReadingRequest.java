package com.example.eam.Maintenance.Predictive.Dto;

import com.example.eam.Enum.MeterType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeterReadingRequest {
    @NotNull
    private Long assetId;
    @NotNull
    private MeterType meterType;
    @NotNull
    private Double readingValue;
    private LocalDateTime readingTime;
    private String notes;
}
