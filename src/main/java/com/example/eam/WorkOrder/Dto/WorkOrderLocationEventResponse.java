package com.example.eam.WorkOrder.Dto;

import com.example.eam.Enum.WorkOrderGeofenceEventType;
import com.example.eam.Enum.WorkOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WorkOrderLocationEventResponse {

    private Long locationLogId;

    private Long workOrderDbId;
    private String workOrderId;

    private Long technicianId;
    private Long teamId;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime observedAt;

    private Integer geofenceRadiusMeters;
    private BigDecimal distanceMeters;
    private boolean insideGeofence;
    private WorkOrderGeofenceEventType eventType;

    private WorkOrderStatus statusBefore;
    private WorkOrderStatus statusAfter;
    private boolean autoCheckIn;
    private boolean autoCheckOut;
}
