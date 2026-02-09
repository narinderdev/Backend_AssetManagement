package com.example.eam.TechnicianTeam.Dto;

import com.example.eam.Enum.TechnicianTeamStatus;
import com.example.eam.Technician.Dto.TechnicianDetailsResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TechnicianTeamDetailsResponse {

    private Long id;
    private String teamName;
    private String teamDescription;
    private TechnicianTeamStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private Long teamLeaderId;
    private String teamLeaderName;
    private String availability;
    private List<TechnicianDetailsResponse> technicians;
}
