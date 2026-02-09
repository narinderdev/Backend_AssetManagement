package com.example.eam.ServiceMaintenance.Dto;

import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.Enum.ServiceRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ServiceRequestResponse {

    private Long id;                        // DB id
    private String requestId;               // Business Request ID

    private LocalDateTime requestDate;

    private String requesterName;
    private String requesterContact;
    private String department;

    private Long assetDbId;
    private String assetId;
    private String assetName;

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
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
}

