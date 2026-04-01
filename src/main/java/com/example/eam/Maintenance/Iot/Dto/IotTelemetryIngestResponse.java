package com.example.eam.Maintenance.Iot.Dto;

import com.example.eam.Maintenance.Iot.Enum.IotAlertSeverity;
import com.example.eam.Maintenance.Iot.Enum.IotAlertType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IotTelemetryIngestResponse {
    private LocalDateTime processedAt;
    private IotAlertSeverity severity;
    private IotAlertType alertType;
    private boolean anomalyDetected;
    private Long alertId;
    private Long serviceRequestId;
    private String message;
}
