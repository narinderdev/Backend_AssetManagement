package com.example.eam.Roles.Service;

import com.example.eam.Roles.Dto.RoleCreateRequest;
import com.example.eam.Roles.Dto.RolePatchRequest;
import com.example.eam.Roles.Dto.RoleResponse;
import com.example.eam.Roles.Entity.AppPermission;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.AppPermissionRepository;
import com.example.eam.Roles.Repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final AppPermissionRepository permissionRepository;

    @Transactional
    public RoleResponse create(RoleCreateRequest req) {
        String name = req.getName().trim();

        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists: " + name);
        }

        Set<AppPermission> permissions = resolvePermissions(req.getPermissionCodes());

        Role role = Role.builder()
                .name(name)
                .description(req.getDescription())
                .active(true)
                .technicianRole(Boolean.TRUE.equals(req.getTechnicianRole()))
                .permissions(new HashSet<>(permissions))
                .build();

        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    @Transactional
    public RoleResponse patch(Long id, RolePatchRequest req) {
        Role role = roleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            String newName = req.getName().trim();
            if (!newName.equalsIgnoreCase(role.getName()) && roleRepository.existsByNameIgnoreCase(newName)) {
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
            Set<AppPermission> permissions = resolvePermissions(req.getPermissionCodes());
            role.getPermissions().clear();
            role.getPermissions().addAll(permissions);
        }

        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RoleResponse get(Long id) {
        Role role = roleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        return toResponse(role);
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> list(Pageable pageable) {
        return roleRepository.findByActiveTrue(pageable).map(this::toResponse);
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        role.setActive(false);
        roleRepository.save(role);
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
}

