package com.example.eam.Maintenance.Emergency.Dto;

import com.example.eam.WorkOrder.Dto.WorkOrderDetailsResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmergencyIncidentResponse {

    private Long id;
    private Long workOrderId;
    private String workOrderNumber;
    private Long assetId;
    private String assetName;
    private String location;
    private String failureDescription;
    private LocalDateTime failureTime;
    private LocalDateTime downtimeStart;
    private LocalDateTime downtimeEnd;
    private String reporter;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private WorkOrderDetailsResponse workOrder;
}
