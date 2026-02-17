package com.example.eam.Security.Service;

import com.example.eam.Enum.SecurityEventCategory;
import com.example.eam.Enum.SecurityEventResult;
import com.example.eam.Enum.SecurityEventType;
import com.example.eam.Security.Dto.*;
import com.example.eam.Security.Entity.SecurityEvent;
import com.example.eam.Security.Repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityDashboardService {

    private final SecurityEventRepository securityEventRepository;

    private static final Set<SecurityEventType> USER_ACTIVITY_TYPES = Set.of(
            SecurityEventType.USER_CREATED,
            SecurityEventType.USER_DISABLED,
            SecurityEventType.USER_ENABLED,
            SecurityEventType.USER_DELETED,
            SecurityEventType.PASSWORD_RESET,
            SecurityEventType.PASSWORD_CHANGED,
            SecurityEventType.USER_ROLE_ASSIGNED,
            SecurityEventType.USER_ROLE_REMOVED,
            SecurityEventType.ACCOUNT_LOCKED,
            SecurityEventType.ACCOUNT_UNLOCKED
    );

    private static final Set<SecurityEventType> ROLE_PERMISSION_TYPES = Set.of(
            SecurityEventType.ROLE_CREATED,
            SecurityEventType.ROLE_UPDATED,
            SecurityEventType.ROLE_PERMISSION_UPDATED
    );

    @Transactional(readOnly = true)
    public SecurityDashboardResponse getDashboard(String period, int limit) {
        LocalDateTime from;
        LocalDateTime to = endOfNow();
        period = period != null ? period.trim().toUpperCase() : "THIS_WEEK";
        switch (period) {
            case "THIS_MONTH":
                from = startOfMonth();
                break;
            case "THIS_YEAR":
                from = startOfYear();
                break;
            case "THIS_WEEK":
            default:
                period = "THIS_WEEK";
                from = startOfWeek();
                break;
        }
        SecurityDashboardSliceDto slice = buildSlice(period, from, to, limit);
        return SecurityDashboardResponse.builder()
                .slice(slice)
                .build();
    }

    private SecurityDashboardSliceDto buildSlice(String period, LocalDateTime from, LocalDateTime to, int limit) {
        KpiSummaryDto kpis = KpiSummaryDto.builder()
                .newUsersAdded(count(SecurityEventType.USER_CREATED, from, to))
                .usersRemovedOrDisabled(count(Set.of(SecurityEventType.USER_DISABLED, SecurityEventType.USER_DELETED), from, to))
                .newRolesAdded(count(SecurityEventType.ROLE_CREATED, from, to))
                .roleChanges(count(Set.of(SecurityEventType.ROLE_UPDATED, SecurityEventType.USER_ROLE_ASSIGNED, SecurityEventType.USER_ROLE_REMOVED, SecurityEventType.ROLE_PERMISSION_UPDATED), from, to))
                .permissionChanges(count(SecurityEventType.ROLE_PERMISSION_UPDATED, from, to))
                .build();

        List<UserActivityDto> userActivity = securityEventRepository
                .findTop20ByEventTypeInAndCreatedAtBetweenOrderByCreatedAtDesc(USER_ACTIVITY_TYPES, from, to)
                .stream()
                .limit(limit)
                .map(this::toUserActivity)
                .toList();

        List<RolePermissionChangeDto> roleChanges = securityEventRepository
                .findTop20ByEventTypeInAndCreatedAtBetweenOrderByCreatedAtDesc(ROLE_PERMISSION_TYPES, from, to)
                .stream()
                .limit(limit)
                .map(this::toRolePermissionChange)
                .toList();

        List<SecurityLogEntryDto> securityLog = securityEventRepository
                .findTop50ByOrderByCreatedAtDesc()
                .stream()
                .filter(e -> !e.getCreatedAt().isBefore(from) && !e.getCreatedAt().isAfter(to))
                .limit(Math.max(limit, 20))
                .map(this::toLogEntry)
                .toList();

        return SecurityDashboardSliceDto.builder()
                .period(period)
                .kpiSummary(kpis)
                .recentUserActivity(userActivity)
                .rolePermissionChanges(roleChanges)
                .securityLog(securityLog)
                .build();
    }

    private LocalDateTime startOfWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        return monday.atStartOfDay();
    }

    private LocalDateTime startOfMonth() {
        LocalDate today = LocalDate.now();
        return today.withDayOfMonth(1).atStartOfDay();
    }

    private LocalDateTime startOfYear() {
        LocalDate today = LocalDate.now();
        return today.withDayOfYear(1).atStartOfDay();
    }

    private LocalDateTime endOfNow() {
        return LocalDateTime.now();
    }

    private long count(SecurityEventType type, LocalDateTime from, LocalDateTime to) {
        return count(Set.of(type), from, to);
    }

    private long count(Set<SecurityEventType> types, LocalDateTime from, LocalDateTime to) {
        return securityEventRepository.countByEventTypeInAndCreatedAtBetween(types, from, to);
    }

    private UserActivityDto toUserActivity(SecurityEvent e) {
        return UserActivityDto.builder()
                .actionType(e.getEventType().name())
                .targetUserName(e.getTargetName())
                .performedBy(e.getPerformedBy())
                .dateTime(e.getCreatedAt())
                .details(e.getDetails())
                .status(e.getResult() != null ? e.getResult().name() : SecurityEventResult.SUCCESS.name())
                .build();
    }

    private RolePermissionChangeDto toRolePermissionChange(SecurityEvent e) {
        return RolePermissionChangeDto.builder()
                .actionType(e.getEventType().name())
                .roleName(e.getTargetName())
                .roleId(e.getTargetId())
                .performedBy(e.getPerformedBy())
                .dateTime(e.getCreatedAt())
                .details(e.getDetails())
                .addedPermissions(List.of())   // details field can be parsed by clients
                .removedPermissions(List.of())
                .build();
    }

    private SecurityLogEntryDto toLogEntry(SecurityEvent e) {
        return SecurityLogEntryDto.builder()
                .eventType(e.getEventType().name())
                .category(e.getCategory().name())
                .targetType(e.getTargetType().name())
                .performedBy(e.getPerformedBy())
                .dateTime(e.getCreatedAt())
                .result(e.getResult() != null ? e.getResult().name() : SecurityEventResult.SUCCESS.name())
                .targetName(e.getTargetName())
                .details(e.getDetails())
                .build();
    }
}
