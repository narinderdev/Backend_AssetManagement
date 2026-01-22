package com.example.eam.WorkOrder.Dto;


import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkOrderSource;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.WorkType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class WorkOrderPatchRequest {

    private Long assetId;
    private String location;

    private WorkType workType;
    private PriorityLevel priority;
    private String workRequestTypeCode;

    private String woTitle;
    private String descriptionScope;

    private String planner;
    private Long assignedTechnicianId;
    private Long assignedTeamId;
    private Boolean clearTechnicianAssignment;
    private Boolean clearTeamAssignment;

    private LocalDateTime plannedStartDateTime;
    private LocalDateTime plannedEndDateTime;
    private LocalDate targetCompletionDate;

    private BigDecimal estimatedLaborHours;
    private BigDecimal estimatedMaterialCost;
    private BigDecimal estimatedTotalCost;

    private WorkOrderStatus status;
    private WorkOrderSource source;
}
