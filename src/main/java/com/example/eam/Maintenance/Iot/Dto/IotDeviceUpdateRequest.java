package com.example.eam.Maintenance.Iot.Dto;

import lombok.Data;

@Data
public class IotDeviceUpdateRequest {
    private String deviceName;
    private Long assetId;
    private String location;
    private Boolean enabled;

    // If true and authToken is empty, a new token will be generated and returned once.
    private Boolean rotateToken;
    private String authToken;
}
