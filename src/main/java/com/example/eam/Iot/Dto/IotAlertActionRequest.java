package com.example.eam.Iot.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IotAlertActionRequest {
    private String reason;
    private LocalDateTime suppressedUntil;
}

