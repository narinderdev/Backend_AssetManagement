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
    private Long teamId;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String notes;
}
