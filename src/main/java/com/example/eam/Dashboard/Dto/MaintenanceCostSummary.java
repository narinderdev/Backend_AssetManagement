package com.example.eam.Dashboard.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MaintenanceCostSummary {

    private String period;
    private List<MaintenanceCostPoint> data;
}
