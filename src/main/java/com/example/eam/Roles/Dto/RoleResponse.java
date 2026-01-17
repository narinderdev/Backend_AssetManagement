package com.example.eam.Roles.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private boolean technicianRole;

    private Set<String> permissionCodes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

