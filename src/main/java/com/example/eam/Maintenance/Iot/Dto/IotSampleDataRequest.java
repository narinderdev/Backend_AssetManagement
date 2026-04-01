package com.example.eam.Maintenance.Iot.Dto;

import com.example.eam.Enum.MeterType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IotSampleDataRequest {
    @NotBlank
    private String deviceUid;

    @NotNull
    private MeterType meterType;

    @Min(1)
    private Integer count = 10;
}
