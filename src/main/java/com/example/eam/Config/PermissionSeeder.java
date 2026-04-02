package com.example.eam.Config;


import com.example.eam.Enum.PermissionAction;
import com.example.eam.Enum.PermissionModule;
import com.example.eam.Roles.Entity.*;
import com.example.eam.Roles.Repository.AppPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(1)
public class PermissionSeeder implements ApplicationRunner {

    private final AppPermissionRepository permissionRepository;

    @Override
    public void run(ApplicationArguments args) {

        List<AppPermission> toInsert = new ArrayList<>();
        int sort = 1;

        // CRUD modules (create/view/update/delete)
        seedCrud(toInsert, PermissionModule.ASSET, "asset", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.ASSET_TYPE, "asset_type", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.SERVICE_REQUEST, "service_request", sort); sort += 4;
        seedApprove(toInsert, PermissionModule.SERVICE_REQUEST, "service_request", sort++);
        seedCrud(toInsert, PermissionModule.WORK_ORDER, "work_order", sort); sort += 4;
        seedApprove(toInsert, PermissionModule.WORK_ORDER, "work_order", sort++);
        seedCrud(toInsert, PermissionModule.WORK_ORDER_TYPE, "work_order_type", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.CORRECTIVE_MAINTENANCE, "corrective_maintenance", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.PREVENTIVE_MAINTENANCE, "preventive_maintenance", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.MATERIAL_REQUISITION, "material_requisition", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.PURCHASE_ORDER, "purchase_order", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.GOODS_RECEIPT_NOTE, "goods_receipt_note", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.VENDOR, "vendor", sort); sort += 4;
        seedApprove(toInsert, PermissionModule.VENDOR, "vendor", sort++);
        seedCrud(toInsert, PermissionModule.INVENTORY, "inventory", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.TECHNICIAN, "technician", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.TECHNICIAN_TEAM, "technician_team", sort); sort += 4;
        seedCrud(toInsert, PermissionModule.IOT, "iot", sort); sort += 4;
        seedViewOnly(toInsert, PermissionModule.IOT, "iot_dashboard", sort++);
        seedViewOnly(toInsert, PermissionModule.IOT, "iot_alerts", sort++);
        seedOne(toInsert, PermissionModule.IOT, PermissionAction.UPDATE, "update_iot_alerts", "Update Iot Alerts", sort++);
        seedViewOnly(toInsert, PermissionModule.DASHBOARD, "dashboard", sort++);
        seedReport(toInsert, PermissionModule.REPORTS, "reports", sort); sort += 2;

        // Access-only modules (no CRUD)
        seedAccess(toInsert, PermissionModule.MANAGE_USERS, "manage_users", sort++); 
        seedAccess(toInsert, PermissionModule.MANAGE_ROLES, "manage_roles", sort++);
        seedViewOnly(toInsert, PermissionModule.INVITE_USER, "invite_user", sort++);

        if (!toInsert.isEmpty()) {
            permissionRepository.saveAll(toInsert);
        }
    }

    private void seedCrud(List<AppPermission> toInsert, PermissionModule module, String moduleCode, int startSort) {
        seedOne(toInsert, module, PermissionAction.CREATE, "create_" + moduleCode, "Create " + title(moduleCode), startSort);
        seedOne(toInsert, module, PermissionAction.VIEW,   "view_" + moduleCode,   "View " + title(moduleCode), startSort + 1);
        seedOne(toInsert, module, PermissionAction.UPDATE, "update_" + moduleCode, "Update " + title(moduleCode), startSort + 2);
        seedOne(toInsert, module, PermissionAction.DELETE, "delete_" + moduleCode, "Delete " + title(moduleCode), startSort + 3);
    }

    private void seedAccess(List<AppPermission> toInsert, PermissionModule module, String code, int sortOrder) {
        seedOne(toInsert, module, PermissionAction.ACCESS, code, "Access " + title(code), sortOrder);
    }

    private void seedApprove(List<AppPermission> toInsert, PermissionModule module, String moduleCode, int sortOrder) {
        seedOne(toInsert, module, PermissionAction.APPROVE, "approve_" + moduleCode, "Approve " + title(moduleCode), sortOrder);
    }

    private void seedReport(List<AppPermission> toInsert, PermissionModule module, String moduleCode, int startSort) {
        seedOne(toInsert, module, PermissionAction.VIEW, "view_" + moduleCode, "View " + title(moduleCode), startSort);
        seedOne(toInsert, module, PermissionAction.EXPORT, "export_" + moduleCode, "Export " + title(moduleCode), startSort + 1);
    }

    private void seedViewOnly(List<AppPermission> toInsert, PermissionModule module, String moduleCode, int sortOrder) {
        seedOne(toInsert, module, PermissionAction.VIEW, "view_" + moduleCode, "View " + title(moduleCode), sortOrder);
    }

    private void seedOne(List<AppPermission> toInsert,
                         PermissionModule module,
                         PermissionAction action,
                         String code,
                         String label,
                         int sortOrder) {

        if (permissionRepository.existsByCode(code)) return;

        toInsert.add(AppPermission.builder()
                .code(code)                 // EXACT permission string you want
                .module(module)
                .action(action)
                .label(label)
                .description("Allows user to " + action.name().toLowerCase() + " in " + module.name())
                .active(true)
                .sortOrder(sortOrder)
                .build());
    }

    private String title(String snake) {
        String[] parts = snake.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1).toLowerCase())
              .append(" ");
        }
        return sb.toString().trim();
    }
}

