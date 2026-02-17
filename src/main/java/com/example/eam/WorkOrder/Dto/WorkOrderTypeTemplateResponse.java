package com.example.eam.WorkOrder.Dto;

import com.example.eam.Enum.CostTreatment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkOrderTypeTemplateResponse {
    private Long id;
    private String workOrderType;
    private String defaultGlAccount;
    private String defaultUtilityAccount;
    private CostTreatment costTreatment;
    private String laborGlAccount;
    private String laborUtilityAccount;
    private String inventoryGlAccount;
    private String inventoryUtilityAccount;
    private boolean createAsset;
    private String propertyUnit;
    private String propertyGroup;
    private String retirementUnit;
    private String functionalClass;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
