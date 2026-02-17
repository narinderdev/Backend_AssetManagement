package com.example.eam.Security.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SecurityLogEntryDto {
    private String eventType;
    private String category;
    private String targetType;
    private String performedBy;
    private LocalDateTime dateTime;
    private String result;
    private String targetName;
    private String details;
}
