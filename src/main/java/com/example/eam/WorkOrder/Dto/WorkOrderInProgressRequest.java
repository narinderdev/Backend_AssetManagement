package com.example.eam.WorkOrder.Dto;

import lombok.Data;

@Data
public class WorkOrderInProgressRequest {
    // Optional override for when the work actually started; defaults to now if not provided
    private java.time.LocalDateTime actualStartDateTime;
}
