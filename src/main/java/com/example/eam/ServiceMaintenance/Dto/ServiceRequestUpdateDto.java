package com.example.eam.ServiceMaintenance.Dto;

import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.Enum.ServiceRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceRequestUpdateDto {

    private String requestId;          // allow change (with uniqueness check)

    private String requesterName;
    private String requesterContact;
    private String department;

    private Long assetId;
    private String location;

    private MaintenanceType maintenanceType;
    private RequestPriority priority;

    private String shortTitle;
    private String problemDescription;

    private LocalDateTime preferredDateTime;
    private Long preferredTechnicianId;
    private Long preferredTeamId;

    private Boolean safetyRisk;
    private String attachmentUrl;

    private ServiceRequestStatus status;
}

