package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderCheckOutRequest {

    private LocalDateTime checkOutAt;

    private String notes;
}
