package com.example.eam.Iot.Dto;

import com.example.eam.Enum.IotRuleOperator;
import lombok.Data;

@Data
public class IotRulePatchRequest {
    private Long assetId;
    private String metricCode;
    private String location;
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

