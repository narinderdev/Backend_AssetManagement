package com.example.eam.User.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.eam.CompanyManagement.Entity.Company;
import com.example.eam.CompanyManagement.Repository.CompanyRepository;
import com.example.eam.Roles.Entity.AppPermission;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.AppPermissionRepository;
import com.example.eam.Roles.Repository.RoleRepository;
import com.example.eam.User.dto.ChangePasswordDto;
import com.example.eam.User.dto.ForgotPasswordDto;
import com.example.eam.User.dto.UserCreateDto;
import com.example.eam.User.dto.UserSummaryDto;
import com.example.eam.User.entity.UserCompany;
import com.example.eam.User.entity.UserRole;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.UserCompanyRepository;
import com.example.eam.User.repository.UserRoleRepository;
import com.example.eam.User.repository.UsersRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityEventRepository securityEventRepository;


    //create user
        public Users register(UserCreateDto dto, Long companyId) {
            Long resolvedCompanyId = resolveActiveCompanyId(companyId);
            ensureCurrentUserCanAccessCompany(resolvedCompanyId);

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
            long adminCount = userRoleRepository
                    .countByRoleNameIgnoreCaseAndCompanyIdAndUserDeletedFalse("Admin", resolvedCompanyId);
            if (adminCount > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin role is already assigned to another user");
            }

            // Assign Admin role to the new user
            Role ownerRole = resolveOrSeedCompanyAdminRole(resolvedCompanyId);

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

            if (dto.getCompanyIds() != null && !dto.getCompanyIds().isEmpty()
                    && !new HashSet<>(dto.getCompanyIds()).contains(resolvedCompanyId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be mapped to the selected company");
            }
            syncUserCompanies(savedUser, List.of(resolvedCompanyId));

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
        public java.util.List<UserSummaryDto> listUsers(Long companyId) {
            Long resolvedCompanyId = resolveActiveCompanyId(companyId);
            ensureCurrentUserCanAccessCompany(resolvedCompanyId);

            return usersRepository.findAllActiveWithRolesByCompanyId(resolvedCompanyId)
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

        private void syncUserCompanies(Users user, List<Long> companyIds) {
            userCompanyRepository.deleteByUser_Id(user.getId());

            if (companyIds == null || companyIds.isEmpty()) {
                return;
            }

            for (Long companyId : new HashSet<>(companyIds)) {
                if (companyId == null) {
                    continue;
                }
                Company company = companyRepository.findById(companyId)
                        .filter(Company::isActive)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found: " + companyId));

                userCompanyRepository.save(UserCompany.builder()
                        .user(user)
                        .company(company)
                        .build());
            }
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

        private Long resolveActiveCompanyId(Long companyId) {
            if (companyId == null || companyId <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required");
            }
            companyRepository.findById(companyId)
                    .filter(Company::isActive)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
            return companyId;
        }

        private void ensureCurrentUserCanAccessCompany(Long companyId) {
            String currentEmail = resolveCurrentUserEmail();
            if (currentEmail == null) {
                return;
            }
            boolean allowed = userCompanyRepository.existsActiveMapping(currentEmail, companyId);
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this company");
            }
        }

        private String resolveCurrentUserEmail() {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                    return null;
                }
                Object principal = auth.getPrincipal();
                if (principal == null) {
                    return null;
                }
                String email = String.valueOf(principal).trim();
                return email.isBlank() ? null : email;
            } catch (Exception ignored) {
                return null;
            }
        }

        private Role resolveOrSeedCompanyAdminRole(Long companyId) {
            Role existing = roleRepository.findFirstByNameIgnoreCaseAndCompanyIdOrderByIdAsc("Admin", companyId)
                    .orElse(null);
            if (existing != null) {
                if (!existing.isActive()) {
                    existing.setActive(true);
                    return roleRepository.save(existing);
                }
                return existing;
            }

            java.util.List<AppPermission> permissions = appPermissionRepository.findByActiveTrueOrderByModuleAscSortOrderAsc();
            Role seeded = Role.builder()
                    .companyId(companyId)
                    .name("Admin")
                    .description("Full access")
                    .active(true)
                    .permissions(new java.util.HashSet<>(permissions))
                    .build();
            return roleRepository.save(seeded);
        }

}
