package com.example.eam.Dashboard.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponse {

    private MetricCard activeWorkOrders;
    private MetricCard overdueTasks;
    private MetricCard inProgressWorkOrders;

    private WorkOrderStatusBreakdown workOrdersByStatus;
    private List<RecentWorkOrderDto> recentWorkOrders;
}
