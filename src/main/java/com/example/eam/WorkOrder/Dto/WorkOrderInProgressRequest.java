package com.example.eam.WorkOrder.Dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WorkOrderInProgressRequest {
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String notes;
}
