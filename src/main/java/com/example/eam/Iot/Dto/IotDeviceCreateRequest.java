package com.example.eam.Iot.Dto;

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

    private Boolean enabled;
}

