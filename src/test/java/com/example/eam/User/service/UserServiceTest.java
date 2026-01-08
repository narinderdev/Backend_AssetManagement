package com.example.eam.User.service;

import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.RoleRepository;
import com.example.eam.User.dto.UserCreateDto;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.UserRoleRepository;
import com.example.eam.User.repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UsersRepository usersRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_rejectsWhenAdminAlreadyAssigned() {
        when(usersRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.empty());
        when(userRoleRepository.countByRole_NameIgnoreCaseAndUser_DeletedFalse("Admin")).thenReturn(1L);

        UserCreateDto dto = new UserCreateDto();
        dto.setFirstName("Admin");
        dto.setLastName("User");
        dto.setEmail("admin@example.com");
        dto.setPassword("secret123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.register(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void register_createsAdminWhenNoneExists() {
        when(usersRepository.findByEmailAndDeletedFalse("admin@example.com")).thenReturn(Optional.empty());
        when(userRoleRepository.countByRole_NameIgnoreCaseAndUser_DeletedFalse("Admin")).thenReturn(0L);
        when(roleRepository.findByNameIgnoreCase("Admin")).thenReturn(Optional.of(Role.builder().id(1L).name("Admin").build()));
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> {
            Users u = invocation.getArgument(0);
            u.setId(1L);
            u.setStatus(UserStatus.ACTIVE);
            return u;
        });

        UserCreateDto dto = new UserCreateDto();
        dto.setFirstName("Admin");
        dto.setLastName("User");
        dto.setEmail("admin@example.com");
        dto.setPassword("secret123");

        Users saved = userService.register(dto);
        assertEquals(1L, saved.getId());
    }

    @Test
    void listUsers_mapsRoles() {
        Role admin = Role.builder().id(1L).name("Admin").build();
        Users user = Users.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        com.example.eam.User.entity.UserRole ur = com.example.eam.User.entity.UserRole.builder()
                .user(user)
                .role(admin)
                .build();
        user.setUserRoles(java.util.List.of(ur));

        lenient().when(usersRepository.findAllActiveWithRoles()).thenReturn(java.util.List.of(user));

        var result = userService.listUsers();
        assertEquals(1, result.size());
        assertEquals("john@example.com", result.get(0).getEmail());
        assertEquals("Admin", result.get(0).getRoles().get(0));
    }
}
