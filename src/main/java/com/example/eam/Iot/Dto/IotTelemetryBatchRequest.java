package com.example.eam.Iot.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class IotTelemetryBatchRequest {
    @NotEmpty
    @Valid
    private List<IotTelemetryReadingRequest> readings;
}

