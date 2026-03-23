package com.example.eam.Auth.dto;

import com.example.eam.User.entity.Users;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private List<CompanySummary> companies;
    private Boolean isCompanySetup;
    private Integer daysUntilPasswordExpiry;
    private Boolean passwordExpired;
    @JsonProperty("mfa_required")
    private Boolean mfaRequired;

    @JsonProperty("mfa_token")
    private String mfaToken;


    public LoginResponseDto(String token, Users user, Long technicianId, Boolean technician, Boolean teamLeader, List<TeamSummary> leaderTeams) {
        this(token, user, technicianId, technician, teamLeader, leaderTeams, null, null);
    }

    public LoginResponseDto(String token, Users user, Long technicianId, Boolean technician, Boolean teamLeader,
                            List<TeamSummary> leaderTeams, Integer daysUntilPasswordExpiry, Boolean passwordExpired) {
        this(token, user, technicianId, technician, teamLeader, leaderTeams, daysUntilPasswordExpiry, passwordExpired, null, null, null, null);
    }

    public LoginResponseDto(String token, Users user, Long technicianId, Boolean technician, Boolean teamLeader,
                            List<TeamSummary> leaderTeams, Integer daysUntilPasswordExpiry, Boolean passwordExpired,
                            Boolean mfaRequired, String mfaToken) {
        this(token, user, technicianId, technician, teamLeader, leaderTeams, daysUntilPasswordExpiry, passwordExpired, mfaRequired, mfaToken, null, null);
    }

    public LoginResponseDto(String token, Users user, Long technicianId, Boolean technician, Boolean teamLeader,
                            List<TeamSummary> leaderTeams, Integer daysUntilPasswordExpiry, Boolean passwordExpired,
                            Boolean mfaRequired, String mfaToken, List<CompanySummary> companies, Boolean isCompanySetup) {
        this.token = token;
        this.user = user;
        this.technicianId = technicianId;
        this.technician = technician;
        this.teamLeader = teamLeader;
        this.leaderTeams = leaderTeams;
        this.daysUntilPasswordExpiry = daysUntilPasswordExpiry;
        this.passwordExpired = passwordExpired;
        this.mfaRequired = mfaRequired;
        this.mfaToken = mfaToken;
        this.companies = companies;
        this.isCompanySetup = isCompanySetup;
    }

    public LoginResponseDto(Boolean mfaRequired, String mfaToken) {
        this.mfaRequired = mfaRequired;
        this.mfaToken = mfaToken;
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

    @Getter
    @NoArgsConstructor
    public static class CompanySummary {
        private Long id;
        private String companyLegalName;
        private String companyTradeName;
        private String companyNumber;
        private String address;
        private String city;
        private String country;
        private String postalCode;

        public CompanySummary(Long id, String companyLegalName, String companyTradeName, String companyNumber,
                              String address, String city, String country, String postalCode) {
            this.id = id;
            this.companyLegalName = companyLegalName;
            this.companyTradeName = companyTradeName;
            this.companyNumber = companyNumber;
            this.address = address;
            this.city = city;
            this.country = country;
            this.postalCode = postalCode;
        }
    }
}
