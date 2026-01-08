package com.example.eam.User.service;

import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.RoleRepository;
import com.example.eam.User.dto.InviteUserRequest;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.UserRoleRepository;
import com.example.eam.User.repository.UsersRepository;
import com.example.eam.Common.EmailService;
import com.example.eam.Common.EmailTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private UsersRepository usersRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private EmailTemplateService emailTemplateService;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void inviteUser_rejectsSecondAdmin() {
        Users existing = Users.builder().id(1L).email("existing@example.com").status(UserStatus.ACTIVE).build();
        when(usersRepository.findByEmail("newadmin@example.com")).thenReturn(Optional.empty());
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> {
            Users u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        Role adminRole = Role.builder().id(5L).name("Admin").build();
        when(roleRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(adminRole));
        when(userRoleRepository.countByRole_NameIgnoreCaseAndUser_IdNotAndUser_DeletedFalse("Admin", 2L))
                .thenReturn(1L);

        InviteUserRequest req = new InviteUserRequest();
        req.setFirstName("New");
        req.setLastName("Admin");
        req.setEmail("newadmin@example.com");
        req.setRoleIds(List.of(5L));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> invitationService.inviteUser(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
