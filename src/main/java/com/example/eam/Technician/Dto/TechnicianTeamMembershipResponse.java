package com.example.eam.Technician.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TechnicianTeamMembershipResponse {

    private Long teamId;
    private String teamName;
    private boolean teamLeader;
    private List<String> teamLeaderNames;
}
