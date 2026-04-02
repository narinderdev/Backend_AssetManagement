package com.example.eam.Iot.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IotTelemetryBatchResponse {
    private int receivedCount;
    private int acceptedCount;
    private int duplicateCount;
    private int failedCount;
    private List<String> errors;
}

