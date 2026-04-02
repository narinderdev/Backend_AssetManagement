package com.example.eam.Iot.Dto;

import com.example.eam.Enum.IotRuleOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IotRuleCreateRequest {

    @NotNull
    private Long assetId;

    @NotBlank
    private String metricCode;

    private String location;

    @NotNull
    private IotRuleOperator ruleOperator;

    private Double lowThreshold;
    private Double mediumThreshold;
    private Double highThreshold;
    private Double criticalThreshold;

    private Integer cooldownMinutes;
    private Double spikeDelta;
    private Integer consecutiveAbnormalCount;
    private Boolean autoCreateServiceRequest;
    private Boolean active;
}

