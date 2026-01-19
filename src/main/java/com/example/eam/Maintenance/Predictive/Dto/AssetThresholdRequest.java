package com.example.eam.Maintenance.Predictive.Dto;

import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.PriorityLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssetThresholdRequest {
    @NotNull
    private Long assetId;
    private String location;
    @NotNull
    private MeterType meterType;
    private Double warningThreshold;
    private Double criticalThreshold;
    private Boolean autoCreateWo = true;
    private PriorityLevel defaultPriority = PriorityLevel.MEDIUM;
    private Integer cooldownHours = 24;
}
