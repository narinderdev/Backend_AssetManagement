package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class WorkOrderScheduleRequest {

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
