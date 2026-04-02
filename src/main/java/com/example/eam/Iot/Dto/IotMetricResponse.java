package com.example.eam.Iot.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IotMetricResponse {
    private Long id;
    private String metricCode;
    private String metricName;
    private String unit;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

