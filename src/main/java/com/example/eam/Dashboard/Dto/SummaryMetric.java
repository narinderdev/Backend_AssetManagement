package com.example.eam.Dashboard.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SummaryMetric {

    private long count;

    @JsonProperty("change_percentage")
    private Double changePercentage;

    @JsonProperty("change_direction")
    private String changeDirection;

    @JsonProperty("comparison_period")
    private String comparisonPeriod;
}
