package com.example.eam.WorkOrder.Dto;

import com.example.eam.Enum.CostTreatment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkOrderTypeTemplateCreateRequest {

    @NotBlank
    private String workOrderType;

    private String defaultGlAccount;
    private String defaultUtilityAccount;

    @NotNull
    private CostTreatment costTreatment;

    private String laborGlAccount;
    private String laborUtilityAccount;
    private String inventoryGlAccount;
    private String inventoryUtilityAccount;

    private Boolean active;
}
