package com.example.eam.VoiceAI.Dto;

import com.example.eam.ServiceMaintenance.Dto.ServiceRequestResponse;
import com.example.eam.VoiceAI.Enum.VoiceCallIntent;
import com.example.eam.VoiceAI.Enum.VoiceIntakeOutcome;
import com.example.eam.WorkOrder.Dto.WorkOrderDetailsResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VoiceIntakeResponse {

    private Long voiceIntakeId;
    private VoiceCallIntent intent;
    private VoiceIntakeOutcome outcome;

    private ServiceRequestResponse serviceRequest;
    private WorkOrderDetailsResponse workOrder;
    private VoiceInquiryStatusResponse inquiryStatus;

    private boolean escalationRequired;
    private String escalationReason;

    private String assignedTechnician;
    private LocalDateTime expectedResponseAt;
    private String customerConfirmationMessage;
}
