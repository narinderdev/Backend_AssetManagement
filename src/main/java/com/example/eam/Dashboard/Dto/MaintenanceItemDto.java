package com.example.eam.Dashboard.Dto;

import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.Enum.WorkType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MaintenanceItemDto {

    @JsonProperty("wo_db_id")
    private Long workOrderDbId;

    @JsonProperty("wo_id")
    private String workOrderId;

    private String title;
    private String asset;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    private PriorityLevel priority;
    private WorkOrderStatus status;

    @JsonProperty("work_type")
    private WorkType workType;

    private boolean preventive;
}
