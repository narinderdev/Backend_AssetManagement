package com.example.eam.Iot.Dto;

import com.example.eam.Enum.IotAlertSeverity;
import com.example.eam.Enum.IotAlertStatus;
import com.example.eam.Enum.IotAnomalyType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IotAlertResponse {
    private Long id;
    private Long deviceId;
    private String deviceUid;
    private Long assetId;
    private String assetName;
    private Long ruleId;
    private Long metricId;
    private String metricCode;
    private Double latestValue;
    private Double thresholdValue;
    private IotAlertSeverity severity;
    private IotAnomalyType anomalyType;
    private IotAlertStatus status;
    private String location;
    private String message;
    private Long linkedServiceRequestDbId;
    private String linkedServiceRequestId;
    private LocalDateTime occurredAt;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime lastNormalAt;
    private Integer healthyStreak;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime suppressedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

