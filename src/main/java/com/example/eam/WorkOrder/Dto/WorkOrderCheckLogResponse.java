package com.example.eam.WorkOrder.Dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCheckLogResponse {
    private Long id;
    private Long technicianId;
    private String technicianName;
    private Long teamId;
    private String teamName;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String notes;
    private java.util.List<WorkOrderPauseWindowResponse> pauses;
}
