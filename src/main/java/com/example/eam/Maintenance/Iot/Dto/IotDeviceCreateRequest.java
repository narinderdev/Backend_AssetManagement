package com.example.eam.Maintenance.Iot.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IotDeviceCreateRequest {
    @NotBlank
    private String deviceUid;

    @NotBlank
    private String deviceName;

    private Long assetId;
    private String location;
    private Boolean enabled = true;

    // Optional. If omitted, system generates a token and returns it once.
    private String authToken;
}
