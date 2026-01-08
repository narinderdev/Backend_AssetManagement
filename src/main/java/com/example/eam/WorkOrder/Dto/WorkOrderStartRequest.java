package com.example.eam.WorkOrder.Dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class WorkOrderStartRequest {
    private LocalDateTime actualStartDateTime;
    private String checkInNotes;
}
