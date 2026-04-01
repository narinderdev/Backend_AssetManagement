package com.example.eam.Maintenance.Iot.Dto;

import com.example.eam.Enum.MeterType;
import com.example.eam.Maintenance.Iot.Enum.IotAlertSeverity;
import com.example.eam.Maintenance.Iot.Enum.IotAlertStatus;
import com.example.eam.Maintenance.Iot.Enum.IotAlertType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IotAlertResponse {
    private Long id;
    private String deviceUid;
    private String deviceName;
    private Long assetId;
    private String assetName;
    private String location;
    private MeterType meterType;
    private Double readingValue;
    private Double thresholdValue;
    private IotAlertSeverity severity;
    private IotAlertType alertType;
    private IotAlertStatus status;
    private String message;
    private Long serviceRequestId;
    private String serviceRequestCode;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
