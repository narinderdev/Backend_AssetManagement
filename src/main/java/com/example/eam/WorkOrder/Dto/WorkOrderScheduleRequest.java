package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkOrderScheduleRequest {

    private Long assignedTechnicianId;
    private Long assignedTeamId;

    @NotNull
    @FutureOrPresent
    private LocalDateTime plannedStartDateTime;

    @NotNull
    @FutureOrPresent
    private LocalDateTime plannedEndDateTime;

    private String planner;
    private String preCheckNotes;

    private List<WorkOrderMaterialPlanRequest> plannedMaterials;
}
