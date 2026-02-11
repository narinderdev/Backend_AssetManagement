package com.example.eam.Auth.dto;

import com.example.eam.User.entity.Users;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponseDto {
    private String token;
    private Users user;
    private Long technicianId;
    private Boolean technician;
    private Boolean teamLeader;
    private List<TeamSummary> leaderTeams;
    private Integer daysUntilPasswordExpiry;
    private Boolean passwordExpired;

    public LoginResponseDto(String token, Users user, Long technicianId, Boolean technician, Boolean teamLeader, List<TeamSummary> leaderTeams) {
        this(token, user, technicianId, technician, teamLeader, leaderTeams, null, null);
    }

    public LoginResponseDto(String token, Users user, Long technicianId, Boolean technician, Boolean teamLeader,
                            List<TeamSummary> leaderTeams, Integer daysUntilPasswordExpiry, Boolean passwordExpired) {
        this.token = token;
        this.user = user;
        this.technicianId = technicianId;
        this.technician = technician;
        this.teamLeader = teamLeader;
        this.leaderTeams = leaderTeams;
        this.daysUntilPasswordExpiry = daysUntilPasswordExpiry;
        this.passwordExpired = passwordExpired;
    }

    @Getter
    @NoArgsConstructor
    public static class TeamSummary {
        private Long id;
        private String name;

        public TeamSummary(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
