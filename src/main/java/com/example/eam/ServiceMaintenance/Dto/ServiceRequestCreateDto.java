package com.example.eam.ServiceMaintenance.Dto;

import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ServiceRequestCreateDto {

    // Optional: user can provide their own requestId
    private String requestId;

    @NotBlank
    private String requesterName;

    private String requesterContact;
    private String department;

    private Long assetId;          // Optional

    private String location;       // Optional; auto from asset if null

    @NotNull
    private MaintenanceType maintenanceType;

    @NotNull
    private RequestPriority priority;

    @NotBlank
    private String shortTitle;

    @NotBlank
    private String problemDescription;

    private LocalDate preferredStartDate;
    private LocalTime preferredStartTime;
    private LocalDate preferredEndDate;
    private LocalTime preferredEndTime;

    // optional assignment preferences
    private Long preferredTechnicianId;
    private Long preferredTeamId;

    private Boolean safetyRisk;

    private String attachmentUrl;
}

