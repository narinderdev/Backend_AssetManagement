package com.example.eam.Iot.Dto;

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
    private boolean enabled;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

