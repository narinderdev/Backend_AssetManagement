package com.example.eam.WorkOrder.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkOrderRejectRequest {

    @NotBlank(message = "rejectedBy is required")
    private String rejectedBy;

    private String rejectionReason;
}
