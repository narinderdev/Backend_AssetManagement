package com.example.eam.User.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.RoleRepository;
import com.example.eam.User.dto.ChangePasswordDto;
import com.example.eam.User.dto.ForgotPasswordDto;
import com.example.eam.User.dto.UserCreateDto;
import com.example.eam.User.dto.UserSummaryDto;
import com.example.eam.User.entity.UserRole;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.UserRoleRepository;
import com.example.eam.User.repository.UsersRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.eam.Enum.SecurityEventCategory;
import com.example.eam.Enum.SecurityEventResult;
import com.example.eam.Enum.SecurityEventType;
import com.example.eam.Enum.SecurityTargetType;
import com.example.eam.Security.Entity.SecurityEvent;
import com.example.eam.Security.Repository.SecurityEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityEventRepository securityEventRepository;


    //create user
        public Users register(UserCreateDto dto) {
            // Destructure the DTO (Java-style)
            String firstName = dto.getFirstName();
            String lastName = dto.getLastName();
            String password = dto.getPassword();
            String email = dto.getEmail().trim().toLowerCase();

            // Check if the user already exists
            Optional<Users> existingUser = usersRepository.findByEmailAndDeletedFalse(email);
            if (existingUser.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists with this email");
            }

            // Enforce single Admin assignment
            long adminCount = userRoleRepository.countByRole_NameIgnoreCaseAndUser_DeletedFalse("Admin");
            if (adminCount > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin role is already assigned to another user");
            }

            // Assign Admin role to the new user
            Role ownerRole = roleRepository.findByNameIgnoreCase("Admin")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin role not found"));

            // Create the user object and assign the role
            Users newUser = new Users();
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setEmail(email);
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setDeleted(false);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setUpdatedAt(Instant.now());
            

            // Save the user
            Users savedUser = usersRepository.save(newUser);  // This should now correctly insert into the user_roles join table


            // Create the UserRole object to link the user with the role
            UserRole userRole = UserRole.builder()
                    .user(savedUser)
                    .role(ownerRole)
                    .build();

            // Save the UserRole to establish the relationship between the user and the role
            userRoleRepository.save(userRole);

            logEvent(SecurityEventType.USER_CREATED, savedUser, "Admin user created");
            return savedUser;
        }

        @Transactional
        public void changePassword(String emailFromRequest, ChangePasswordDto dto) {
            String normalizedEmail = emailFromRequest != null ? emailFromRequest.trim().toLowerCase() : null;
            if (normalizedEmail == null || normalizedEmail.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
            }

            Users user = usersRepository.findByEmailAndDeletedFalse(normalizedEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is not set for this user");
            }

            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
            }

            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            user.setUpdatedAt(Instant.now());
            usersRepository.save(user);
            logEvent(SecurityEventType.PASSWORD_CHANGED, user, "Password changed via change-password");
        }

        @Transactional
        public void forgotPassword(ForgotPasswordDto dto) {
            String normalizedEmail = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : null;
            if (normalizedEmail == null || normalizedEmail.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
            }

            Users user = usersRepository.findByEmailAndDeletedFalse(normalizedEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not active");
            }

            if (user.getPassword() != null && !user.getPassword().isBlank()
                    && passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from current password");
            }

            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            user.setUpdatedAt(Instant.now());
            usersRepository.save(user);
            logEvent(SecurityEventType.PASSWORD_CHANGED, user, "Password changed via forgot-password");
        }

        @Transactional(readOnly = true)
        public java.util.List<UserSummaryDto> listUsers() {
            return usersRepository.findAllActiveWithRoles()
                    .stream()
                    .map(this::toSummary)
                    .toList();
        }

        private UserSummaryDto toSummary(Users user) {
            String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
            String last = user.getLastName() != null ? user.getLastName().trim() : "";
            String fullName = (first + " " + last).trim();

            java.util.List<String> roles = user.getUserRoles() != null
                    ? user.getUserRoles().stream()
                        .map(UserRole::getRole)
                        .filter(java.util.Objects::nonNull)
                        .map(Role::getName)
                        .toList()
                    : java.util.List.of();

            return new UserSummaryDto(
                    user.getId(),
                    fullName.isEmpty() ? null : fullName,
                    user.getEmail(),
                    user.getStatus(),
                    roles
            );
        }

        private void logEvent(SecurityEventType type, Users user, String details) {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType(type)
                    .category(SecurityEventCategory.USER)
                    .targetType(SecurityTargetType.USER)
                    .targetId(user.getId())
                    .targetName(user.getEmail())
                    .performedBy(resolveCurrentUser())
                    .result(SecurityEventResult.SUCCESS)
                    .details(details)
                    .build();
            securityEventRepository.save(event);
        }

        private String resolveCurrentUser() {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() != null) {
                    return String.valueOf(auth.getPrincipal());
                }
            } catch (Exception ignored) { }
            return "system";
        }

}
