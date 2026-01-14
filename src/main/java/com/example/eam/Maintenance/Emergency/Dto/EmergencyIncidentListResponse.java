package com.example.eam.Maintenance.Emergency.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmergencyIncidentListResponse {

    private List<EmergencyIncidentResponse> incidents;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
