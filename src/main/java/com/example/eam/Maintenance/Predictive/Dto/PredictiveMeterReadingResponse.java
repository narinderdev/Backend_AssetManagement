package com.example.eam.Maintenance.Predictive.Dto;

import com.example.eam.Enum.MeterType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PredictiveMeterReadingResponse {
    private Long id;
    private MeterType meterType;
    private Double readingValue;
    private LocalDateTime readingTime;
    private String severity;
    private String notes;
    private LocalDateTime createdAt;
}
