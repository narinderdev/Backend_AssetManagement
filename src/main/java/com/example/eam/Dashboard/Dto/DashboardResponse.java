package com.example.eam.Dashboard.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponse {

    @JsonProperty("summary_metrics")
    private SummaryMetrics summaryMetrics;

    @JsonProperty("work_orders_by_status")
    private WorkOrderStatusSummary workOrdersByStatus;

    @JsonProperty("maintenance_cost_summary")
    private MaintenanceCostSummary maintenanceCostSummary;

    @JsonProperty("recent_work_orders")
    private List<RecentWorkOrderDto> recentWorkOrders;

    private DashboardMetadata metadata;
}
