package com.example.eam.Security.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RolePermissionChangeDto {
    private String actionType;
    private String roleName;
    private Long roleId;
    private String performedBy;
    private LocalDateTime dateTime;
    private List<String> addedPermissions;
    private List<String> removedPermissions;
    private String details;
}
