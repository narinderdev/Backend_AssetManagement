package com.example.eam.WorkOrder.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkOrderTeamMemberResponse {
    Long technicianId;
    String technicianName;
    String email;
    Boolean teamLeader;
}
