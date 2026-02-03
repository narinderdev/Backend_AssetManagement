package com.example.eam.Technician.Dto;

import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Enum.TechnicianWorkStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TechnicianDetailsResponse {

    private Long id;
    private String technicianId;
    private String badgeNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private TechnicianType technicianType;
    private String skills;
    private String phoneNumber;
    private String email;
    private String address;
    private TechnicianStatus status;
    private TechnicianWorkStatus workStatus;
    private LocalDate hireDate;
    private String workShift;
    private String technicianPhotoUrl;
    private String certificateUrl;
    private LocalDate certificateIssueDate;
    private LocalDate certificateExpiryDate;
    private LocalDate terminationDate;
    private String certifications;
    private String notes;
    private boolean teamLeader;
    private List<TechnicianTeamMembershipResponse> teamMemberships;
}
