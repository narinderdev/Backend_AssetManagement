package com.example.eam.Maintenance.Iot.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IotDashboardResponse {
    private long registeredDeviceCount;
    private long onlineDeviceCount;
    private long activeAlertCount;
    private List<IotDeviceResponse> deviceStatus;
    private List<IotAlertResponse> activeAlerts;
    private List<IotAlertResponse> recentIssues;
}
