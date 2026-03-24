package com.example.eam.User.seed;

import com.example.eam.CompanyManagement.Entity.Company;
import com.example.eam.CompanyManagement.Repository.CompanyRepository;
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
    private final CompanyRepository companyRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<AppPermission> permissions = appPermissionRepository.findByActiveTrueOrderByModuleAscSortOrderAsc();
        HashSet<AppPermission> permissionSet = new HashSet<>(permissions);

        // Ensure a global Admin role exists and is granted every permission.
        Role globalAdmin = roleRepository.findFirstByNameIgnoreCaseAndCompanyIdIsNullOrderByIdAsc("Admin")
                .orElseGet(() -> Role.builder()
                        .companyId(null)
                        .name("Admin")
                        .description("Full access")
                        .active(true)
                        .build()
                );
        globalAdmin.setPermissions(new HashSet<>(permissionSet));
        roleRepository.save(globalAdmin);

        // Ensure every active company has an Admin role with all active permissions.
        for (Company company : companyRepository.findByActiveTrue()) {
            Role companyAdmin = roleRepository.findFirstByNameIgnoreCaseAndCompanyIdOrderByIdAsc("Admin", company.getId())
                    .orElseGet(() -> Role.builder()
                            .companyId(company.getId())
                            .name("Admin")
                            .description("Full access")
                            .active(true)
                            .build()
                    );

            companyAdmin.setPermissions(new HashSet<>(permissionSet));
            roleRepository.save(companyAdmin);
        }

        System.out.println("Global and company Admin roles synced with all permissions.");
    }
}
