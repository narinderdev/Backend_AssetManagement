package com.example.eam.Roles.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PermissionCatalogObjectResponse {
    private String name;
    private List<PermissionResponse> permissions;
}
