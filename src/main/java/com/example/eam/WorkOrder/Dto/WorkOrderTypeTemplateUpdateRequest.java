package com.example.eam.WorkOrder.Dto;

import com.example.eam.Enum.CostTreatment;
import lombok.Data;

@Data
public class WorkOrderTypeTemplateUpdateRequest {

    private String workOrderType;

    private String defaultGlAccount;
    private String defaultUtilityAccount;

    private CostTreatment costTreatment;

    private String laborGlAccount;
    private String laborUtilityAccount;
    private String inventoryGlAccount;
    private String inventoryUtilityAccount;

    private Boolean active;
}
