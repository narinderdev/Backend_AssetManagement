package com.example.eam.Iot.Dto;

import lombok.Data;

@Data
public class IotDevicePatchRequest {

    private String deviceName;

    private Long assetId;

    private String location;

    private Boolean enabled;
}

