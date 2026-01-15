package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderCheckInRequest {

    private Long technicianId;
    private Long teamId;

    @NotNull
    private LocalDateTime checkInAt;

    private String notes;
}
