package com.example.eam.Iot.Dto;

import lombok.Data;

@Data
public class IotMetricPatchRequest {
    private String metricCode;
    private String metricName;
    private String unit;
    private String description;
    private Boolean active;
}

