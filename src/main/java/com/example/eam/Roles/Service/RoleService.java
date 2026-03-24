package com.example.eam.Roles.Service;

import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Roles.Dto.RoleCreateRequest;
import com.example.eam.Roles.Dto.RolePatchRequest;
import com.example.eam.Roles.Dto.RoleResponse;
import com.example.eam.Roles.Entity.AppPermission;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.AppPermissionRepository;
import com.example.eam.Roles.Repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.eam.Enum.SecurityEventCategory;
import com.example.eam.Enum.SecurityEventResult;
import com.example.eam.Enum.SecurityEventType;
import com.example.eam.Enum.SecurityTargetType;
import com.example.eam.Security.Entity.SecurityEvent;
import com.example.eam.Security.Repository.SecurityEventRepository;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final AppPermissionRepository permissionRepository;
    private final SecurityEventRepository securityEventRepository;

    @Transactional
    public RoleResponse create(RoleCreateRequest req) {
        Long companyId = requireCompanyId();
        String name = req.getName().trim();

        if (roleRepository.existsByNameIgnoreCaseAndCompanyId(name, companyId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists: " + name);
        }

        Set<AppPermission> permissions = resolvePermissions(req.getPermissionCodes());

        Role role = Role.builder()
                .companyId(companyId)
                .name(name)
                .description(req.getDescription())
                .active(true)
                .technicianRole(Boolean.TRUE.equals(req.getTechnicianRole()))
                .permissions(new HashSet<>(permissions))
                .build();

        Role saved = roleRepository.save(role);
        logEvent(SecurityEventType.ROLE_CREATED, saved, permissions, Set.of());
        return toResponse(saved);
    }

    @Transactional
    public RoleResponse patch(Long id, RolePatchRequest req) {
        Long companyId = requireCompanyId();
        Role role = roleRepository.findByIdAndActiveTrueAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            String newName = req.getName().trim();
            if (!newName.equalsIgnoreCase(role.getName())
                    && roleRepository.existsByNameIgnoreCaseAndCompanyId(newName, companyId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists: " + newName);
            }
            role.setName(newName);
        }

        if (req.getDescription() != null) {
            role.setDescription(req.getDescription());
        }

        if (req.getActive() != null) {
            role.setActive(req.getActive());
        }

        if (req.getTechnicianRole() != null) {
            role.setTechnicianRole(req.getTechnicianRole());
        }

        if (req.getPermissionCodes() != null) {
            Set<String> before = role.getPermissions().stream().map(AppPermission::getCode).collect(Collectors.toSet());
            Set<AppPermission> permissions = resolvePermissions(req.getPermissionCodes());
            role.getPermissions().clear();
            role.getPermissions().addAll(permissions);
            Set<String> after = permissions.stream().map(AppPermission::getCode).collect(Collectors.toSet());
            logPermissionDelta(role, before, after);
        }

        Role saved = roleRepository.save(role);
        logEvent(SecurityEventType.ROLE_UPDATED, saved, role.getPermissions(), Set.of());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RoleResponse get(Long id) {
        Long companyId = requireCompanyId();
        Role role = roleRepository.findByIdAndActiveTrueAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        return toResponse(role);
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> list(Pageable pageable) {
        Long companyId = requireCompanyId();
        return roleRepository.findByActiveTrueAndCompanyId(companyId, pageable).map(this::toResponse);
    }

    @Transactional
    public void delete(Long id) {
        Long companyId = requireCompanyId();
        Role role = roleRepository.findByIdAndActiveTrueAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        role.setActive(false);
        roleRepository.save(role);
        logEvent(SecurityEventType.ROLE_UPDATED, role, role.getPermissions(), Set.of(), "Role deactivated");
    }

    private Long requireCompanyId() {
        return CompanyContextHolder.getCompanyId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required"));
    }

    private Set<AppPermission> resolvePermissions(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "permissionCodes cannot be empty");
        }

        List<AppPermission> found = permissionRepository.findByCodeIn(codes);

        if (found.size() != codes.size()) {
            Set<String> foundCodes = new HashSet<>(found.stream().map(AppPermission::getCode).toList());
            Set<String> missing = new HashSet<>(codes);
            missing.removeAll(foundCodes);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid permission codes: " + missing);
        }

        return new HashSet<>(found);
    }

    private RoleResponse toResponse(Role r) {
        Set<String> codes = r.getPermissions().stream().map(AppPermission::getCode).collect(java.util.stream.Collectors.toSet());
        return RoleResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .active(r.isActive())
                .technicianRole(r.isTechnicianRole())
                .permissionCodes(codes)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private void logPermissionDelta(Role role, Set<String> before, Set<String> after) {
        Set<String> added = new HashSet<>(after);
        added.removeAll(before);
        Set<String> removed = new HashSet<>(before);
        removed.removeAll(after);
        if (added.isEmpty() && removed.isEmpty()) return;
        String details = "added=" + String.join(",", added) + "; removed=" + String.join(",", removed);
        logEvent(SecurityEventType.ROLE_PERMISSION_UPDATED, role, Set.of(), Set.of(), details);
    }

    private void logEvent(SecurityEventType type, Role role, Set<AppPermission> currentPerms, Set<AppPermission> removedPerms) {
        logEvent(type, role, currentPerms, removedPerms, null);
    }

    private void logEvent(SecurityEventType type, Role role, Set<AppPermission> currentPerms, Set<AppPermission> removedPerms, String extraDetails) {
        String performedBy = resolveCurrentUser();
        String details = extraDetails;
        if (details == null) {
            Set<String> codes = currentPerms != null ? currentPerms.stream().map(AppPermission::getCode).collect(Collectors.toSet()) : Set.of();
            details = "permissions=" + String.join(",", codes);
        }
        SecurityEvent event = SecurityEvent.builder()
                .eventType(type)
                .category(SecurityEventCategory.ROLE)
                .targetType(SecurityTargetType.ROLE)
                .targetId(role.getId())
                .targetName(role.getName())
                .performedBy(performedBy)
                .result(SecurityEventResult.SUCCESS)
                .details(details)
                .build();
        securityEventRepository.save(event);
    }

    private String resolveCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() != null) {
                return String.valueOf(auth.getPrincipal());
            }
        } catch (Exception ignored) { }
        return "system";
    }
}

