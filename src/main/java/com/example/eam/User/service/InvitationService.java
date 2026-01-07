package com.example.eam.User.service;

import com.example.eam.Common.EmailService;
import com.example.eam.Common.EmailTemplateService;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.RoleRepository;
import com.example.eam.User.dto.InviteUserRequest;
import com.example.eam.User.dto.SetPasswordDto;
import com.example.eam.User.entity.UserRole;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.UserRoleRepository;
import com.example.eam.User.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmailTemplateService emailTemplateService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.invite.accept-url}")
    private String acceptUrl;

    @Value("${app.frontend.set-password-url}")
    private String setPasswordUrl;

    @Value("${spring.application.name:eam}")
    private String applicationName;

    @Transactional
    public Users inviteUser(InviteUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Optional<Users> existingOpt = usersRepository.findByEmail(email);

        if (existingOpt.isPresent()) {
            Users existing = existingOpt.get();
            if (!existing.isDeleted() && existing.getStatus() != UserStatus.INVITED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists with this email");
            }
        }

        Users user = existingOpt.orElseGet(Users::new);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(email);
        user.setStatus(UserStatus.INVITED);
        user.setDeleted(false);
        user.setPassword(null);

        Users saved = usersRepository.save(user);

        // reset role links then add requested roles
        userRoleRepository.deleteByUserId(saved.getId());
        Set<Long> uniqueRoleIds = new HashSet<>(request.getRoleIds());
        for (Long roleId : uniqueRoleIds) {
            Role role = roleRepository.findByIdAndActiveTrue(roleId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Role not found: " + roleId
                    ));
            userRoleRepository.save(UserRole.builder()
                    .user(saved)
                    .role(role)
                    .build());
        }

        String inviteLink = buildInviteLink(email);
        String emailHtml = emailTemplateService.buildInviteEmail(
                Optional.ofNullable(user.getFirstName()).orElse("there"),
                applicationName,
                inviteLink
        );

        emailService.sendWithAttachment(
                email,
                "You're invited to join " + applicationName,
                emailHtml,
                null
        );

        return saved;
    }

    public void validateInvite(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        Users user = usersRepository.findByEmailAndDeletedFalse(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getStatus() != UserStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already used or invalid");
        }
    }

    @Transactional
    public void setPassword(SetPasswordDto dto) {
        String email = dto.getEmail().trim().toLowerCase();

        Users user = usersRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invite"));

        if (user.getStatus() != UserStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already used or invalid");
        }

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        usersRepository.save(user);
    }

    public String getSetPasswordRedirectUrl(String email) {
        String encoded = URLEncoder.encode(email.trim().toLowerCase(), StandardCharsets.UTF_8);
        return setPasswordUrl + (setPasswordUrl.contains("?") ? "&" : "?") + "email=" + encoded;
    }

    private String buildInviteLink(String email) {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String separator = acceptUrl.contains("?") ? "&" : "?";
        return acceptUrl + separator + "email=" + encodedEmail;
    }
}
