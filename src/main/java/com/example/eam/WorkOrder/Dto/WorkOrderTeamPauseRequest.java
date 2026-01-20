package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderTeamPauseRequest {

    @NotNull
    private Long teamId;

    private LocalDateTime pauseAt;

    private String notes;
}
