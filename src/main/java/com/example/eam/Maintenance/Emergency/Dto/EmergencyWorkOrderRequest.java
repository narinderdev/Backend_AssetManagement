package com.example.eam.Maintenance.Emergency.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmergencyWorkOrderRequest {
    private Long assetId;
    private String location;
    @NotBlank
    private String failureDescription;
    private LocalDateTime failureTime;
    private String reporter;
    private Boolean sendNotification = true;
}
