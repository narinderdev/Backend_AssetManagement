package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderTeamResumeRequest {

    @NotNull
    private Long teamId;

    private LocalDateTime resumeAt;

    private String notes;
}
