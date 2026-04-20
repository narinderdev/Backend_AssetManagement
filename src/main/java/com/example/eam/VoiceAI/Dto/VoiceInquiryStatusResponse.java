package com.example.eam.VoiceAI.Dto;

import com.example.eam.Enum.ServiceRequestStatus;
import com.example.eam.Enum.WorkOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VoiceInquiryStatusResponse {

    private String serviceRequestId;
    private ServiceRequestStatus serviceRequestStatus;

    private String workOrderId;
    private String workOrderNumber;
    private WorkOrderStatus workOrderStatus;

    private String assignedTechnician;
    private LocalDateTime expectedResponseAt;
}
