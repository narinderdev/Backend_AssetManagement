package com.example.eam.Iot.Dto;

import com.example.eam.Enum.IotRuleOperator;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IotRuleResponse {
    private Long id;
    private Long assetId;
    private String assetName;
    private Long metricId;
    private String metricCode;
    private String metricName;
    private String location;
    private IotRuleOperator ruleOperator;
    private Double lowThreshold;
    private Double mediumThreshold;
    private Double highThreshold;
    private Double criticalThreshold;
    private Integer cooldownMinutes;
    private Double spikeDelta;
    private Integer consecutiveAbnormalCount;
    private boolean autoCreateServiceRequest;
    private boolean active;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

