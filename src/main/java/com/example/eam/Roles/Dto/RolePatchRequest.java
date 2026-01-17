package com.example.eam.Roles.Dto;

import lombok.Data;

import java.util.Set;

@Data
public class RolePatchRequest {
    private String name;
    private String description;
    private Boolean active;
    private Boolean technicianRole;

    // If provided => replace role permissions
    private Set<String> permissionCodes;
}

