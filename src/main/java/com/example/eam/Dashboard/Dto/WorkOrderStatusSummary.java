package com.example.eam.Dashboard.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkOrderStatusSummary {

    @JsonProperty("new")
    private long newCount;

    @JsonProperty("in_progress")
    private long inProgress;

    private long completed;

    private long total;
}
