package com.example.eam.Iot.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IotDeviceCreateResponse {
    private IotDeviceResponse device;
    private String deviceSecret;
}

