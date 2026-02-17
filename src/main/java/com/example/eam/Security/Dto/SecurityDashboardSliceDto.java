package com.example.eam.Security.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SecurityDashboardSliceDto {
    private String period; // THIS_WEEK / THIS_MONTH / THIS_YEAR
    private KpiSummaryDto kpiSummary;
    private List<UserActivityDto> recentUserActivity;
    private List<RolePermissionChangeDto> rolePermissionChanges;
    private List<SecurityLogEntryDto> securityLog;
}
