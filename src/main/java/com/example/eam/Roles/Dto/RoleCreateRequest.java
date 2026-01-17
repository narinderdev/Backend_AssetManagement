package com.example.eam.Roles.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class RoleCreateRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Boolean technicianRole;

    @NotEmpty
    private Set<String> permissionCodes;
}

