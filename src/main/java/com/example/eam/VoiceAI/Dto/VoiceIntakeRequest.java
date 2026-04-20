package com.example.eam.VoiceAI.Dto;

import com.example.eam.Enum.MaintenanceType;
import com.example.eam.Enum.RequestPriority;
import com.example.eam.VoiceAI.Enum.VoiceCallIntent;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoiceIntakeRequest {

    private String externalCallId;
    private VoiceCallIntent intent;

    private String requesterName;
    private String requesterPhoneNumber;
    private String requesterContact;
    private String department;

    private String issueDescription;
    private String shortTitle;
    private String transcript;
    private Object conversation;

    private Long assetDbId;
    private String assetId;
    private String assetName;
    private String location;

    private RequestPriority urgency;
    private MaintenanceType maintenanceType;
    private Boolean safetyRisk;

    private LocalDateTime preferredStartDateTime;
    private LocalDateTime preferredEndDateTime;
    private Integer expectedDurationHours;

    private Long preferredTechnicianId;
    private Long preferredTeamId;
    private List<String> skillTags;

    private String inquiryServiceRequestId;
    private String inquiryWorkOrderId;

    private Boolean unclearIntent;
}
