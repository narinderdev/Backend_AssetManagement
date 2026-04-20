package com.example.eam.Iot.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IotMetricCreateRequest {

    @NotBlank
    private String metricCode;

    @NotBlank
    private String metricName;

    private String unit;

    private String description;

    private Boolean active;
}

