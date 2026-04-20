package com.example.eam.Iot.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class IotMqttTelemetryMessage {

    @NotBlank
    private String deviceUid;

    @NotBlank
    private String deviceSecret;

    @NotEmpty
    @Valid
    private List<IotTelemetryReadingRequest> readings;
}
