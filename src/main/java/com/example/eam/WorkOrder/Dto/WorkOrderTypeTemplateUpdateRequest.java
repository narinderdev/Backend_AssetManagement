package com.example.eam.WorkOrder.Dto;

import com.example.eam.Enum.CostTreatment;
import jakarta.validation.constraints.Size;
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

    private Boolean createAsset;

    @Size(max = 128)
    private String propertyUnit;

    @Size(max = 128)
    private String propertyGroup;

    @Size(max = 128)
    private String retirementUnit;

    @Size(max = 128)
    private String functionalClass;

    private Boolean active;
}
