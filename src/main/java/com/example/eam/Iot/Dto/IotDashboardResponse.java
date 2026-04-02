package com.example.eam.Iot.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IotDashboardResponse {
    private long totalDevices;
    private long onlineDevices;
    private long offlineDevices;
    private long activeAlerts;
    private long criticalAlerts;
    private List<IotAlertResponse> recentIssues;
}

