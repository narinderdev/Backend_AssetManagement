package com.example.eam.Security.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiSummaryDto {
    private long newUsersAdded;
    private long usersRemovedOrDisabled;
    private long newRolesAdded;
    private long roleChanges;
    private long permissionChanges;
}
