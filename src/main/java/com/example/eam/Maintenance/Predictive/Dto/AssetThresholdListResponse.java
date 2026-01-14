package com.example.eam.Maintenance.Predictive.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssetThresholdListResponse {
    private List<AssetThresholdResponse> thresholds;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
