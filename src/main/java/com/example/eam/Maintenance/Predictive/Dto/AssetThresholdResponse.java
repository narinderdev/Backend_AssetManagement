package com.example.eam.Maintenance.Predictive.Dto;

import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.PriorityLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetThresholdResponse {
    private Long id;
    private Long assetId;
    private String assetName;
    private MeterType meterType;
    private Double warningThreshold;
    private Double criticalThreshold;
    private Boolean autoCreateWo;
    private PriorityLevel defaultPriority;
    private Integer cooldownHours;
    private String lastTriggeredSeverity;
}
