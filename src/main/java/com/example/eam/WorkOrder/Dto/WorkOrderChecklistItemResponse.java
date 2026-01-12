package com.example.eam.WorkOrder.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkOrderChecklistItemResponse {
    private Long id;
    private String itemText;
    private Boolean required;
    private Boolean completed;
}
