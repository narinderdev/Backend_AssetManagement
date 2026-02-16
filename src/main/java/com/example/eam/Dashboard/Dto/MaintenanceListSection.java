package com.example.eam.Dashboard.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MaintenanceListSection {
    private long count;
    private List<MaintenanceItemDto> items;
}
