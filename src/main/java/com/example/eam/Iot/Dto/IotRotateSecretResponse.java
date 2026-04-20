package com.example.eam.Iot.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IotRotateSecretResponse {
    private Long deviceId;
    private String deviceUid;
    private String newSecret;
}

