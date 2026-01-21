package com.example.eam.Dashboard.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SummaryMetrics {

    @JsonProperty("open_service_requests")
    private SummaryMetric openServiceRequests;

    @JsonProperty("active_work_orders")
    private SummaryMetric activeWorkOrders;

    @JsonProperty("active_material_requisitions")
    private SummaryMetric activeMaterialRequisitions;

    @JsonProperty("critical_assets_down")
    private SummaryMetric criticalAssetsDown;
}
