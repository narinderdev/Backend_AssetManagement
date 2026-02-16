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

    @JsonProperty("new_service_requests")
    private List<NewServiceRequestDto> newServiceRequests;

    @JsonProperty("requests_not_accepted_count")
    private Long requestsNotAcceptedCount;

    @JsonProperty("upcoming_maintenance")
    private MaintenanceListSection upcomingMaintenance;

    @JsonProperty("past_due_maintenance")
    private MaintenanceListSection pastDueMaintenance;

    private DashboardMetadata metadata;
}
