package com.example.eam.Maintenance.Iot.Dto;

import com.example.eam.Enum.MeterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IotTelemetryIngestRequest {
    @NotNull
    private Long companyId;

    @NotBlank
    private String deviceUid;

    @NotBlank
    private String authToken;

    @NotNull
    private MeterType meterType;

    @NotNull
    private Double readingValue;

    private LocalDateTime readingTime;
    private String location;
    private String notes;
}
