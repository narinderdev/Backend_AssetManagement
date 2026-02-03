package com.example.eam.Dashboard.Dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TechnicianActivityDto {
    String technicianName;
    String activity;
    String timeAgo;
}
