package com.example.eam.User.seed;

import com.example.eam.Roles.Entity.AppPermission;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.AppPermissionRepository;
import com.example.eam.Roles.Repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final AppPermissionRepository appPermissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        // Ensure an Admin role exists and is granted every permission
        Role admin = roleRepository.findByNameIgnoreCase("Admin")
                .orElseGet(() -> Role.builder()
                        .name("Admin")
                        .description("Full access")
                        .active(true)
                        .build()
                );

        List<AppPermission> permissions = appPermissionRepository.findAll();
        admin.setPermissions(new HashSet<>(permissions));

        roleRepository.save(admin);

        System.out.println("Admin role synced with all permissions.");
    }
}
