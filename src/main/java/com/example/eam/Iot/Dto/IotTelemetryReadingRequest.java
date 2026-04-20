package com.example.eam.Iot.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IotTelemetryReadingRequest {
    private String eventId;
    private Long assetId;
    @NotBlank
    private String metricCode;
    @NotNull
    private Double readingValue;
    private LocalDateTime observedAt;
    private String location;
}

