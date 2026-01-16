package com.example.eam.WorkOrder.Dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderResumeRequest {
    private LocalDateTime resumeAt;
    private String notes;
    private Long technicianId;
    private Long teamId;
}
