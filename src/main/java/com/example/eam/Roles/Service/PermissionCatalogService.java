package com.example.eam.Roles.Service;

import com.example.eam.Enum.PermissionAction;
import com.example.eam.Enum.PermissionModule;
import com.example.eam.Roles.Dto.PermissionCatalogClassResponse;
import com.example.eam.Roles.Dto.PermissionCatalogObjectResponse;
import com.example.eam.Roles.Dto.PermissionCatalogResponse;
import com.example.eam.Roles.Dto.PermissionResponse;
import com.example.eam.Roles.Entity.AppPermission;
import com.example.eam.Roles.Repository.AppPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionCatalogService {

    private final AppPermissionRepository permissionRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public PermissionCatalogResponse getCatalog() {
        List<AppPermission> perms = permissionRepository.findByActiveTrueOrderByModuleAscSortOrderAsc();

        Map<PermissionModule, List<AppPermission>> grouped = perms.stream()
                .collect(Collectors.groupingBy(AppPermission::getModule, () -> new EnumMap<>(PermissionModule.class), Collectors.toList()));

        List<PermissionCatalogClassResponse> classes = loadFromDatabase(grouped);

        return PermissionCatalogResponse.builder()
                .classes(classes)
                .build();
    }

    private List<PermissionCatalogClassResponse> loadFromDatabase(Map<PermissionModule, List<AppPermission>> grouped) {
        try {
            List<ClassRow> classRows = jdbcTemplate.query(
                    """
                    SELECT id, name
                    FROM dbo.permission_classes
                    WHERE active = 1
                    ORDER BY sort_order, name
                    """,
                    (rs, rowNum) -> new ClassRow(rs.getLong("id"), rs.getString("name"))
            );

            List<ObjectRow> objectRows = jdbcTemplate.query(
                    """
                    SELECT class_id, name, module, view_only
                    FROM dbo.permission_objects
                    WHERE active = 1
                    ORDER BY class_id, sort_order, name
                    """,
                    (rs, rowNum) -> new ObjectRow(
                            rs.getLong("class_id"),
                            rs.getString("name"),
                            parseModule(rs.getString("module")),
                            rs.getBoolean("view_only")
                    )
            );

            Map<Long, List<ObjectRow>> objectsByClassId = objectRows.stream()
                    .filter(row -> row.module() != null)
                    .collect(Collectors.groupingBy(ObjectRow::classId, LinkedHashMap::new, Collectors.toList()));

            return classRows.stream()
                    .map(classRow -> permissionClass(
                            classRow.name(),
                            objectsByClassId.getOrDefault(classRow.id(), List.of()).stream()
                                    .map(obj -> permissionObject(obj.name(), grouped, obj.module(), obj.viewOnly()))
                                    .toList()
                    ))
                    .toList();
        } catch (DataAccessException ignored) {
            return defaultCatalog(grouped);
        }
    }

    private PermissionCatalogClassResponse permissionClass(String name, List<PermissionCatalogObjectResponse> objects) {
        return PermissionCatalogClassResponse.builder()
                .name(name)
                .objects(objects)
                .build();
    }

    private PermissionCatalogObjectResponse permissionObject(String objectName,
                                                             Map<PermissionModule, List<AppPermission>> grouped,
                                                             PermissionModule module,
                                                             boolean viewOnly) {
        List<PermissionResponse> permissions = grouped.getOrDefault(module, List.of()).stream()
                .filter(p -> !viewOnly || p.getAction() == PermissionAction.VIEW)
                .map(this::toResponse)
                .toList();

        return PermissionCatalogObjectResponse.builder()
                .name(objectName)
                .permissions(permissions)
                .build();
    }

    private PermissionResponse toResponse(AppPermission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .module(p.getModule())
                .action(p.getAction())
                .label(p.getLabel())
                .description(p.getDescription())
                .build();
    }

    private PermissionModule parseModule(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PermissionModule.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<PermissionCatalogClassResponse> defaultCatalog(Map<PermissionModule, List<AppPermission>> grouped) {
        return List.of(
                permissionClass("Dashboard", List.of(
                        permissionObject("Maintenance Dashboard", grouped, PermissionModule.DASHBOARD, true),
                        permissionObject("Security Dashboard", grouped, PermissionModule.DASHBOARD, true),
                        permissionObject("Budget Dashboard", grouped, PermissionModule.DASHBOARD, true),
                        permissionObject("IoT Dashboard", grouped, PermissionModule.IOT, true)
                )),
                permissionClass("Asset", List.of(
                        permissionObject("Asset Type", grouped, PermissionModule.ASSET_TYPE, false),
                        permissionObject("Asset", grouped, PermissionModule.ASSET, false)
                )),
                permissionClass("Service Request", List.of(
                        permissionObject("Service Request", grouped, PermissionModule.SERVICE_REQUEST, false)
                )),
                permissionClass("Work Order", List.of(
                        permissionObject("Work Order Type", grouped, PermissionModule.WORK_ORDER_TYPE, false),
                        permissionObject("Work Order", grouped, PermissionModule.WORK_ORDER, false)
                )),
                permissionClass("Maintenance", List.of(
                        permissionObject("Preventive", grouped, PermissionModule.PREVENTIVE_MAINTENANCE, false),
                        permissionObject("Corrective", grouped, PermissionModule.CORRECTIVE_MAINTENANCE, false),
                        permissionObject("IoT", grouped, PermissionModule.IOT, false)
                )),
                permissionClass("Inventory", List.of(
                        permissionObject("Warehouse", grouped, PermissionModule.INVENTORY, false),
                        permissionObject("Inventory", grouped, PermissionModule.INVENTORY, false),
                        permissionObject("Inventory Reconcile", grouped, PermissionModule.INVENTORY, false),
                        permissionObject("Inventory Audit Logs", grouped, PermissionModule.INVENTORY, false)
                )),
                permissionClass("Procurement", List.of(
                        permissionObject("Material Requisition", grouped, PermissionModule.MATERIAL_REQUISITION, false),
                        permissionObject("Purchase Order", grouped, PermissionModule.PURCHASE_ORDER, false),
                        permissionObject("Goods Receipt Notes", grouped, PermissionModule.GOODS_RECEIPT_NOTE, false)
                )),
                permissionClass("Vendor", List.of(
                        permissionObject("Vendor", grouped, PermissionModule.VENDOR, false)
                )),
                permissionClass("Technician", List.of(
                        permissionObject("Technician", grouped, PermissionModule.TECHNICIAN, false),
                        permissionObject("Technician Team", grouped, PermissionModule.TECHNICIAN_TEAM, false)
                )),
                permissionClass("Reports", List.of(
                        permissionObject("Inventory Report", grouped, PermissionModule.REPORTS, false),
                        permissionObject("Asset Report", grouped, PermissionModule.REPORTS, false),
                        permissionObject("Transaction Report", grouped, PermissionModule.REPORTS, false),
                        permissionObject("Work Order Report", grouped, PermissionModule.REPORTS, false)
                )),
                permissionClass("Security", List.of(
                        permissionObject("Roles", grouped, PermissionModule.MANAGE_ROLES, false),
                        permissionObject("User", grouped, PermissionModule.MANAGE_USERS, false),
                        permissionObject("MFA", grouped, PermissionModule.MANAGE_USERS, false),
                        permissionObject("Invite User", grouped, PermissionModule.INVITE_USER, true),
                        permissionObject("Security Report", grouped, PermissionModule.REPORTS, false)
                ))
        );
    }

    private record ClassRow(Long id, String name) {
    }

    private record ObjectRow(Long classId, String name, PermissionModule module, boolean viewOnly) {
    }
}

