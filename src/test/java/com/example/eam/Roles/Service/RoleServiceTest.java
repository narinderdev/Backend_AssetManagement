package com.example.eam.Roles.Service;

import com.example.eam.Roles.Dto.RoleCreateRequest;
import com.example.eam.Roles.Dto.RolePatchRequest;
import com.example.eam.Roles.Entity.AppPermission;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.AppPermissionRepository;
import com.example.eam.Roles.Repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AppPermissionRepository permissionRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void create_rejectsDuplicateName() {
        when(roleRepository.existsByNameIgnoreCase("Admin")).thenReturn(true);

        RoleCreateRequest req = new RoleCreateRequest();
        req.setName("Admin");
        req.setPermissionCodes(Set.of("READ"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> roleService.create(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void patch_updatesDescriptionAndPermissions() {
        Role existing = Role.builder()
                .id(1L)
                .name("Viewer")
                .description("old")
                .active(true)
                .permissions(new java.util.HashSet<>())
                .build();

        AppPermission perm = AppPermission.builder().id(2L).code("VIEW").build();

        when(roleRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existing));
        when(permissionRepository.findByCodeIn(Set.of("VIEW"))).thenReturn(List.of(perm));
        when(roleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RolePatchRequest req = new RolePatchRequest();
        req.setDescription("updated");
        req.setPermissionCodes(Set.of("VIEW"));

        var resp = roleService.patch(1L, req);

        assertEquals("updated", resp.getDescription());
        assertFalse(resp.getPermissionCodes().isEmpty());
    }
}
