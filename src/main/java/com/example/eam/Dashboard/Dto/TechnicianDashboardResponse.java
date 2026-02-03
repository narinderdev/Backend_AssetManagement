package com.example.eam.Dashboard.Dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TechnicianDashboardResponse {
    long totalTechnicians;
    long availableToday;
    long onLeave;
    long workOrders;
    List<TechnicianActivityDto> recentActivities;
}
