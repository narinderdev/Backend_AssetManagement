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
public class WorkOrderPauseWindowResponse {
    private LocalDateTime pauseAt;
    private LocalDateTime resumeAt;
}
