package com.example.eam.Roles.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PermissionCatalogResponse {
    private List<PermissionCatalogClassResponse> classes;
}
