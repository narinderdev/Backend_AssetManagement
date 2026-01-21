package com.example.eam.Dashboard.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardMetadata {

    @JsonProperty("generated_at")
    private String generatedAt;

    @JsonProperty("data_freshness")
    private String dataFreshness;
}
