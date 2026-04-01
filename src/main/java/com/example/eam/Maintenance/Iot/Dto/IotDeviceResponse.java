package com.example.eam.Maintenance.Iot.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IotDeviceResponse {
    private Long id;
    private String deviceUid;
    private String deviceName;
    private Long assetId;
    private String assetName;
    private String location;
    private Boolean enabled;
    private LocalDateTime lastSeenAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Returned only on create/rotate.
    private String provisioningToken;
}
