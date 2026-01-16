package com.example.eam.WorkOrder.Dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderPauseRequest {
    private LocalDateTime pauseAt;
    private String notes;
}
