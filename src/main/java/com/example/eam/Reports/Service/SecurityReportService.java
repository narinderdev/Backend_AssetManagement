package com.example.eam.Reports.Service;

import com.example.eam.Reports.Dto.SecurityRoleReportItem;
import com.example.eam.Roles.Entity.AppPermission;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityReportService {

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<SecurityRoleReportItem> getRolePermissionReport() {
        List<Role> roles = roleRepository.findByActiveTrueOrderByNameAsc();

        return roles.stream()
                .map(this::toReportItem)
                .toList();
    }

    private SecurityRoleReportItem toReportItem(Role role) {
        // Map module -> sorted unique actions
        Map<String, Set<String>> moduleActions = new TreeMap<>();

        for (AppPermission perm : role.getPermissions()) {
            if (perm == null || !perm.isActive()) continue;
            String module = perm.getModule().name();
            String action = perm.getAction().name();
            moduleActions.computeIfAbsent(module, k -> new TreeSet<>()).add(action);
        }

        Map<String, List<String>> objects = moduleActions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return SecurityRoleReportItem.builder()
                .role(role.getName())
                .objects(objects)
                .build();
    }
}
