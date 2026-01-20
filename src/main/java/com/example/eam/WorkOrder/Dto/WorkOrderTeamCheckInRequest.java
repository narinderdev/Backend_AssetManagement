package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class WorkOrderTeamCheckInRequest {

    @NotNull
    private Long teamId;

    @NotEmpty
    private List<WorkOrderTeamCheckInEntry> technicians;
}
