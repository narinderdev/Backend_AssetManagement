package com.example.eam.Security.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserActivityDto {
    private String actionType;
    private String targetUserName;
    private String performedBy;
    private LocalDateTime dateTime;
    private String details;
    private String status;
}
