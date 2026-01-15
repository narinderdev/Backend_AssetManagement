package com.example.eam.WorkOrder.Dto;


import com.example.eam.Enum.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "workOrderId",
        "linkedServiceRequestDbId",
        "linkedServiceRequestId",
        "pmPlanId",
        "pmPlanCode",
        "pmDueDate",
        "emergencyIncidentId",
        "assetDbId",
        "assetId",
        "assetName",
        "location",
        "workType",
        "priority",
        "woTitle",
        "descriptionScope",
        "planner",
        "assignedTechnicianId",
        "assignedTechnicianName",
        "assignedTeamId",
        "assignedTeamName",
        "plannedStartDateTime",
        "plannedEndDateTime",
        "actualStartDateTime",
        "actualEndDateTime",
        "targetCompletionDate",
        "estimatedLaborHours",
        "estimatedMaterialCost",
        "estimatedTotalCost",
        "actualLaborHours",
        "actualLaborCost",
        "actualMaterialCost",
        "actualTotalCost",
        "completionNotes",
        "failureDescription",
        "failureCause",
        "remedyAction",
        "downtimeStart",
        "downtimeEnd",
        "reporter",
        "checkInAt",
        "checkOutAt",
        "beforePhotoUrl",
        "afterPhotoUrl",
        "supervisorNotes",
        "approvalNotes",
        "rejectionReason",
        "precheckNotes",
        "approvedBy",
        "approvedAt",
        "rejectedBy",
        "rejectedAt",
        "plannedMaterials",
        "checkLogs",
        "status",
        "source",
        "laborEntries",
        "checklistItems",
        "materialUsages",
        "createdAt",
        "updatedAt"
})
public class WorkOrderDetailsResponse {

    private Long id;
    private String workOrderId;

    private Long linkedServiceRequestDbId;   // ServiceMaintenance.id
    private String linkedServiceRequestId;   // ServiceMaintenance.serviceRequestId (business id)

    private Long pmPlanId;
    private String pmPlanCode;
    private LocalDate pmDueDate;
    private Long emergencyIncidentId;

    private Long assetDbId;                  // Asset.id
    private String assetId;                  // Asset.assetId (business id)
    private String assetName;

    private String location;

    private WorkType workType;
    private PriorityLevel priority;

    private String woTitle;
    private String descriptionScope;

    private String planner;
    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private Long assignedTeamId;
    private String assignedTeamName;

    private LocalDateTime plannedStartDateTime;
    private LocalDateTime plannedEndDateTime;
    private LocalDateTime actualStartDateTime;
    private LocalDateTime actualEndDateTime;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private LocalDate targetCompletionDate;
    private BigDecimal estimatedLaborHours;
    private BigDecimal estimatedMaterialCost;
    private BigDecimal estimatedTotalCost;
    private BigDecimal actualLaborHours;
    private BigDecimal actualLaborCost;
    private BigDecimal actualMaterialCost;
    private BigDecimal actualTotalCost;
    private String completionNotes;
    private String failureDescription;
    private String failureCause;
    private String remedyAction;
    private LocalDateTime downtimeStart;
    private LocalDateTime downtimeEnd;
    private String reporter;
    private String beforePhotoUrl;
    private String afterPhotoUrl;
    private String supervisorNotes;
    private String approvalNotes;
    private String rejectionReason;
    private String precheckNotes;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;

    private java.util.List<WorkOrderCheckLogResponse> checkLogs;

    private WorkOrderStatus status;
    private WorkOrderSource source;

    private List<WorkOrderMaterialPlanResponse> plannedMaterials;
    private List<WorkOrderLaborEntryResponse> laborEntries;
    private List<WorkOrderChecklistItemResponse> checklistItems;
    private List<WorkOrderMaterialUsageResponse> materialUsages;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
