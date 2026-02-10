package com.example.eam.Maintenance.Emergency.Dto;

import com.example.eam.WorkOrder.Dto.WorkOrderMaterialPlanRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class EmergencyWorkOrderRequest {
    private Long assetId;
    private String location;
    @NotBlank
    private String failureDescription;
    private LocalDateTime failureTime;
    private String reporter;
    private Boolean sendNotification = true;

    private Long assignedTechnicianId;
    private Long assignedTeamId;

    @NotNull
    private LocalDate plannedStartDate;

    @NotNull
    private LocalTime plannedStartTime;

    @NotNull
    private LocalDate plannedEndDate;

    @NotNull
    private LocalTime plannedEndTime;

    private String planner;
    private String preCheckNotes;

    private List<WorkOrderMaterialPlanRequest> plannedMaterials;
}
