package com.example.eam.Auth.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.eam.Auth.dto.LoginDto;
import com.example.eam.Auth.dto.LoginResponseDto;
import com.example.eam.Auth.dto.MfaLoginDto;
import com.example.eam.CompanyManagement.Entity.Company;
import com.example.eam.CompanyManagement.Repository.CompanyRepository;
import com.example.eam.Enum.PermissionAction;
import com.example.eam.Enum.PermissionModule;
import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Roles.Entity.AppPermission;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.AppPermissionRepository;
import com.example.eam.Roles.Repository.RoleRepository;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.User.entity.PasswordPolicy;
import com.example.eam.User.entity.UserCompany;
import com.example.eam.User.entity.UserRole;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.PasswordPolicyRepository;
import com.example.eam.User.repository.UserCompanyRepository;
import com.example.eam.User.repository.UserRoleRepository;
import com.example.eam.User.repository.UsersRepository;
import com.example.eam.Enum.DevicePlatform;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamMemberRepository;
import com.example.eam.Technician.Repository.TechnicianDeviceTokenRepository;
import com.example.eam.Technician.Entity.TechnicianDeviceToken;

import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UsersRepository usersRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TechnicianRepository technicianRepository;
    private final TechnicianDeviceTokenRepository technicianDeviceTokenRepository;
    private final TechnicianTeamMemberRepository technicianTeamMemberRepository;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final CompanyRepository companyRepository;
    private final MfaService mfaService;
    private final RoleRepository roleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final Map<String, JdbcTemplate> jdbcTemplates;

    @Value("${app.mfa.token-expiration-ms:300000}")
    private long mfaTokenExpirationMs;


    private static final int DEFAULT_PASSWORD_EXPIRY_DAYS = 90;
    private static final int PASSWORD_EXPIRY_WARNING_DAYS = 7;
    private static final String TECHNICIAN_ROLE_NAME = "Technician";
    private static final String TM_TECHNICIAN_ID_PREFIX = "TM-";
    private static final String CREATE_WORK_ORDER_PERMISSION = "create_work_order";
    private static final String UPDATE_WORK_ORDER_PERMISSION = "update_work_order";
    private static final String VIEW_WORK_ORDER_PERMISSION = "view_work_order";
    private static final String TM_USER_LOGIN_SQL = """
            SELECT TOP 1 first_name, last_name, email, password_hash, active
            FROM tm_users
            WHERE LOWER(email) = LOWER(?)
            """;

    @Transactional
    public LoginResponseDto login(LoginDto dto) {
        String email = dto.getEmail().trim().toLowerCase();

        Users user = usersRepository
                .findByEmailAndDeletedFalse(email)
                .orElseGet(() -> provisionFromTmUserIfValid(email, dto.getPassword())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "User not found"
                        )));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "User is not active"
            );
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid password"
            );
        }

        Integer daysUntilPasswordExpiry = resolveDaysUntilPasswordExpiry(user);

        if (user.isMfaEnabled()) {
            Map<String, Object> claims = new java.util.HashMap<>();
            claims.put("userId", user.getId());
            claims.put("user_id", user.getId());
            claims.put("type", "mfa_pending");
            if (dto.getDeviceToken() != null && !dto.getDeviceToken().trim().isEmpty()) {
                claims.put("deviceToken", dto.getDeviceToken().trim());
            }
            if (dto.getDevicePlatform() != null && !dto.getDevicePlatform().trim().isEmpty()) {
                claims.put("devicePlatform", dto.getDevicePlatform().trim());
            }
            String mfaToken = jwtService.generateToken(user.getEmail(), claims, mfaTokenExpirationMs);
            return buildLoginResponse(user, daysUntilPasswordExpiry, dto.getDeviceToken(), dto.getDevicePlatform(), true, mfaToken);
        }

        return buildLoginResponse(user, daysUntilPasswordExpiry, dto.getDeviceToken(), dto.getDevicePlatform(), null, null);
    }

    @Transactional
    public LoginResponseDto loginWithMfa(MfaLoginDto dto) {
        Claims claims = parseMfaClaims(dto.getMfaToken());
        String email = claims.getSubject();
        Long userId = extractUserId(claims);

        Users user = usersRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!Objects.equals(user.getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA token");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
        }
        if (!user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is not enabled");
        }

        mfaService.checkLoginRateLimit(user.getId());
        boolean codeValid = mfaService.verifyActiveCode(user, dto.getCode());
        if (!codeValid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        Integer daysUntilPasswordExpiry = resolveDaysUntilPasswordExpiry(user);
        String deviceToken = claims.get("deviceToken", String.class);
        String devicePlatform = claims.get("devicePlatform", String.class);
        return buildLoginResponse(user, daysUntilPasswordExpiry, deviceToken, devicePlatform, null, null);
    }

    private LoginResponseDto buildLoginResponse(Users user, Integer daysUntilPasswordExpiry, String deviceToken, String devicePlatform,
                                                Boolean mfaRequired, String mfaToken) {
        List<String> roles = new ArrayList<>(user.getUserRoles()
                .stream()
                .map(ur -> ur.getRole().getName())
                .toList());
        boolean isAdmin = roles.stream().anyMatch(this::isAdminRoleName);

        List<LoginResponseDto.CompanySummary> companies = new ArrayList<>(userCompanyRepository
                .findByUser_IdAndCompany_ActiveTrue(user.getId())
                .stream()
                .map(UserCompany::getCompany)
                .filter(Objects::nonNull)
                .map(this::toCompanySummary)
                .toList());

        boolean hasTechnicianRole = user.getUserRoles()
                .stream()
                .anyMatch(ur -> ur.getRole() != null && ur.getRole().isTechnicianRole());
        boolean isTmDbTechnician = isTmDbTechnician(user);
        if (isTmDbTechnician && isWebPlatform(devicePlatform)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "TMDB technician users are not allowed to login from web platform"
            );
        }

        if (isTmDbTechnician && roles.stream().noneMatch(this::isTechnicianRoleName)) {
            roles.add(TECHNICIAN_ROLE_NAME);
        }

        Long technicianId = (hasTechnicianRole || isTmDbTechnician) ? ensureTechnicianProfile(user) : null;
        if (technicianId != null && deviceToken != null && !deviceToken.trim().isEmpty()) {
            registerDeviceToken(technicianId, deviceToken, devicePlatform);
        }
        if (companies.isEmpty() && technicianId != null) {
            findTechnicianCompanySummary(technicianId).ifPresent(companies::add);
        }

        boolean isTechnician = technicianId != null;
        List<LoginResponseDto.TeamSummary> leaderTeams = isTechnician
                ? technicianTeamMemberRepository.findByTechnician_Id(technicianId).stream()
                    .filter(TechnicianTeamMember::isTeamLeader)
                    .map(TechnicianTeamMember::getTeam)
                    .filter(Objects::nonNull)
                    .map(team -> new LoginResponseDto.TeamSummary(team.getId(), team.getTeamName()))
                    .toList()
                : List.of();
        boolean isTeamLeader = !leaderTeams.isEmpty();
        Users responseUser = buildResponseUser(user, isTechnician);

        String token = null;
        if (!Boolean.TRUE.equals(mfaRequired)) {
            token = jwtService.generateToken(
                    user.getEmail(),
                    Map.of(
                            "userId", user.getId(),
                            "email", user.getEmail(),
                            "roles", roles
                    )
            );
        }
        Boolean isCompanySetup = isAdmin ? !companies.isEmpty() : null;
        return new LoginResponseDto(
                token,
                responseUser,
                technicianId,
                isTechnician,
                isTeamLeader,
                leaderTeams,
                daysUntilPasswordExpiry,
                false,
                mfaRequired,
                mfaToken,
                companies,
                isCompanySetup
        );
    }

    private java.util.Optional<LoginResponseDto.CompanySummary> findTechnicianCompanySummary(Long technicianId) {
        if (technicianId == null) {
            return java.util.Optional.empty();
        }
        return technicianRepository.findByIdAndIsDeletedFalse(technicianId)
                .map(Technician::getCompanyId)
                .filter(companyId -> companyId != null && companyId > 0)
                .flatMap(companyRepository::findByIdAndActiveTrue)
                .map(this::toCompanySummary);
    }

    private LoginResponseDto.CompanySummary toCompanySummary(Company company) {
        return new LoginResponseDto.CompanySummary(
                company.getId(),
                company.getCompanyLegalName(),
                company.getCompanyTradeName(),
                company.getCompanyNumber(),
                company.getAddress(),
                company.getCity(),
                company.getCountry(),
                company.getPostalCode()
        );
    }

    private boolean isWebPlatform(String devicePlatform) {
        return devicePlatform != null && "WEB".equalsIgnoreCase(devicePlatform.trim());
    }

    private boolean isTmDbTechnician(Users user) {
        String normalizedEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : null;
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return false;
        }

        return technicianRepository.findByEmailIgnoreCaseAndIsDeletedFalse(normalizedEmail)
                .map(Technician::getTechnicianId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(id -> id.regionMatches(true, 0, TM_TECHNICIAN_ID_PREFIX, 0, TM_TECHNICIAN_ID_PREFIX.length()))
                .orElse(false);
    }

    private boolean isTechnicianRoleName(String roleName) {
        return roleName != null && TECHNICIAN_ROLE_NAME.equalsIgnoreCase(roleName.trim());
    }

    private boolean isAdminRoleName(String roleName) {
        return roleName != null && "Admin".equalsIgnoreCase(roleName.trim());
    }

    private Users buildResponseUser(Users user, boolean isTechnician) {
        if (user == null) {
            return null;
        }
        List<AppPermission> mandatoryPermissions = isTechnician ? resolveMandatoryWorkOrderPermissions() : List.of();
        List<UserRole> responseUserRoles = user.getUserRoles() == null
                ? new ArrayList<>()
                : user.getUserRoles().stream()
                .map(userRole -> copyUserRoleForResponse(userRole, mandatoryPermissions, isTechnician))
                .toList();

        Users responseUser = Users.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .password(user.getPassword())
                .status(user.getStatus())
                .deleted(user.isDeleted())
                .userRoles(new ArrayList<>())
                .userCompanies(user.getUserCompanies() == null ? new ArrayList<>() : new ArrayList<>(user.getUserCompanies()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .mfaEnabled(user.isMfaEnabled())
                .mfaSecret(user.getMfaSecret())
                .mfaSecretTemp(user.getMfaSecretTemp())
                .mfaEmailOtp(user.getMfaEmailOtp())
                .mfaEmailOtpExpiresAt(user.getMfaEmailOtpExpiresAt())
                .mfaEmailVerified(user.isMfaEmailVerified())
                .build();
        responseUser.getUserRoles().addAll(responseUserRoles);
        responseUser.getUserRoles().forEach(role -> role.setUser(responseUser));
        return responseUser;
    }

    private UserRole copyUserRoleForResponse(UserRole userRole, List<AppPermission> mandatoryPermissions, boolean isTechnician) {
        if (userRole == null) {
            return UserRole.builder().build();
        }
        return UserRole.builder()
                .id(userRole.getId())
                .role(copyRoleForResponse(userRole.getRole(), mandatoryPermissions, isTechnician))
                .build();
    }

    private Role copyRoleForResponse(Role role, List<AppPermission> mandatoryPermissions, boolean isTechnician) {
        if (role == null) {
            return null;
        }
        java.util.Set<AppPermission> responsePermissions = copyPermissionSet(role.getPermissions());
        if (isTechnician && role.isTechnicianRole()) {
            for (AppPermission mandatoryPermission : mandatoryPermissions) {
                responsePermissions.removeIf(existing -> hasCode(existing, mandatoryPermission.getCode()));
                responsePermissions.add(copyPermission(mandatoryPermission));
            }
        }

        return Role.builder()
                .id(role.getId())
                .companyId(role.getCompanyId())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.isActive())
                .technicianRole(role.isTechnicianRole())
                .permissions(new java.util.HashSet<>(responsePermissions))
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    private java.util.Set<AppPermission> copyPermissionSet(java.util.Set<AppPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new java.util.HashSet<>();
        }
        java.util.Set<AppPermission> copied = new java.util.HashSet<>();
        permissions.stream()
                .filter(Objects::nonNull)
                .map(this::copyPermission)
                .forEach(copied::add);
        return copied;
    }

    private AppPermission copyPermission(AppPermission permission) {
        return AppPermission.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .module(permission.getModule())
                .action(permission.getAction())
                .label(permission.getLabel())
                .description(permission.getDescription())
                .active(permission.isActive())
                .sortOrder(permission.getSortOrder())
                .build();
    }

    private List<AppPermission> resolveMandatoryWorkOrderPermissions() {
        List<String> mandatoryCodes = List.of(
                CREATE_WORK_ORDER_PERMISSION,
                UPDATE_WORK_ORDER_PERMISSION,
                VIEW_WORK_ORDER_PERMISSION
        );
        java.util.Map<String, AppPermission> foundByCode = appPermissionRepository.findByCodeIn(mandatoryCodes).stream()
                .filter(Objects::nonNull)
                .filter(AppPermission::isActive)
                .filter(permission -> permission.getCode() != null && !permission.getCode().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        permission -> permission.getCode().trim(),
                        permission -> permission,
                        (first, second) -> first
                ));

        List<AppPermission> resolved = new ArrayList<>();
        for (String code : mandatoryCodes) {
            AppPermission permission = foundByCode.get(code);
            resolved.add(permission != null ? permission : fallbackWorkOrderPermission(code));
        }
        return resolved;
    }

    private AppPermission fallbackWorkOrderPermission(String code) {
        PermissionAction action = switch (code) {
            case CREATE_WORK_ORDER_PERMISSION -> PermissionAction.CREATE;
            case UPDATE_WORK_ORDER_PERMISSION -> PermissionAction.UPDATE;
            default -> PermissionAction.VIEW;
        };
        String label = switch (code) {
            case CREATE_WORK_ORDER_PERMISSION -> "Create Work Order";
            case UPDATE_WORK_ORDER_PERMISSION -> "Update Work Order";
            default -> "View Work Order";
        };
        String description = "Allows user to " + action.name().toLowerCase() + " in " + PermissionModule.WORK_ORDER.name();
        return AppPermission.builder()
                .id(null)
                .code(code)
                .module(PermissionModule.WORK_ORDER)
                .action(action)
                .label(label)
                .description(description)
                .active(true)
                .build();
    }

    private boolean hasCode(AppPermission permission, String code) {
        if (permission == null || permission.getCode() == null || code == null) {
            return false;
        }
        return permission.getCode().trim().equalsIgnoreCase(code.trim());
    }

    private java.util.Optional<Users> provisionFromTmUserIfValid(String normalizedEmail, String rawPassword) {
        JdbcTemplate tmJdbcTemplate = jdbcTemplates == null ? null : jdbcTemplates.get("tmJdbcTemplate");
        if (tmJdbcTemplate == null) {
            return java.util.Optional.empty();
        }

        List<TmUserLoginRow> rows = tmJdbcTemplate.query(
                TM_USER_LOGIN_SQL,
                (rs, rowNum) -> new TmUserLoginRow(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getBoolean("active")
                ),
                normalizedEmail
        );

        if (rows.isEmpty()) {
            return java.util.Optional.empty();
        }

        TmUserLoginRow tmUser = rows.get(0);
        if (!tmUser.active()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
        }

        if (tmUser.passwordHash() == null || !passwordEncoder.matches(rawPassword, tmUser.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        Users existing = usersRepository.findByEmailAndDeletedFalse(normalizedEmail).orElse(null);
        if (existing != null) {
            return java.util.Optional.of(existing);
        }

        Role technicianRole = resolveOrCreateTechnicianRole();

        Users userToCreate = Users.builder()
                .firstName(defaultName(tmUser.firstName(), "Technician"))
                .lastName(defaultName(tmUser.lastName(), "User"))
                .email(normalizedEmail)
                .password(tmUser.passwordHash())
                .status(UserStatus.ACTIVE)
                .deleted(false)
                .build();

        try {
            Users saved = usersRepository.save(userToCreate);
            UserRole userRole = userRoleRepository.save(UserRole.builder()
                    .user(saved)
                    .role(technicianRole)
                    .build());
            saved.getUserRoles().add(userRole);
            return java.util.Optional.of(saved);
        } catch (DataIntegrityViolationException ex) {
            return usersRepository.findByEmailAndDeletedFalse(normalizedEmail);
        }
    }

    private Claims parseMfaClaims(String token) {
        try {
            Claims claims = jwtService.parseClaims(token);
            String type = claims.get("type", String.class);
            if (!"mfa_pending".equals(type)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA token");
            }
            return claims;
        } catch (Exception ex) {
            if (ex instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired MFA token");
        }
    }

    private Long extractUserId(Claims claims) {
        Object value = claims.get("userId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        Object altValue = claims.get("user_id");
        if (altValue instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private int resolvePasswordExpiryDays() {
        return passwordPolicyRepository.findTopByOrderByIdAsc()
                .map(PasswordPolicy::getPasswordExpiryDays)
                .filter(days -> days != null && days > 0)
                .orElse(DEFAULT_PASSWORD_EXPIRY_DAYS);
    }

    private LocalDate resolvePasswordChangedAt(Users user) {
        Instant updatedAt = user.getUpdatedAt();
        if (updatedAt != null) {
            return LocalDate.ofInstant(updatedAt, ZoneOffset.UTC);
        }
        Instant createdAt = user.getCreatedAt();
        if (createdAt != null) {
            return LocalDate.ofInstant(createdAt, ZoneOffset.UTC);
        }
        return LocalDate.now();
    }

    private Integer resolveDaysUntilPasswordExpiry(Users user) {
        int expiryDays = resolvePasswordExpiryDays();
        LocalDate passwordChangedAt = resolvePasswordChangedAt(user);
        LocalDate expiryDate = passwordChangedAt.plusDays(expiryDays);
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        if (daysRemaining < 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password expired");
        }
        return daysRemaining <= PASSWORD_EXPIRY_WARNING_DAYS ? (int) daysRemaining : null;
    }

    private Long ensureTechnicianProfile(Users user) {
        String normalizedEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : null;
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User email is required to resolve technician profile");
        }

        return technicianRepository.findByEmailIgnoreCaseAndIsDeletedFalse(normalizedEmail)
                .map(Technician::getId)
                .orElseGet(() -> {
                    Technician technician = Technician.builder()
                            .firstName(defaultName(user.getFirstName(), "Technician"))
                            .lastName(defaultName(user.getLastName(), "User"))
                            .email(normalizedEmail)
                            .badgeNumber(generateUniqueBadgeNumber())
                            .technicianId(generateUniqueTechnicianId())
                            .technicianType(TechnicianType.FULL_TIME)
                            .status(TechnicianStatus.ACTIVE)
                            .build();
                    Technician saved = technicianRepository.save(technician);
                    return saved.getId();
                });
    }

    private void registerDeviceToken(Long technicianId, String rawToken, String platformRaw) {
        String token = rawToken.trim();
        if (token.isEmpty()) return;

        DevicePlatform platform = null;
        if (platformRaw != null) {
            try {
                platform = DevicePlatform.valueOf(platformRaw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Technician technician = technicianRepository.findByIdAndIsDeletedFalse(technicianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Technician not found"));

        TechnicianDeviceToken entity = null;
        if (platform != null) {
            entity = technicianDeviceTokenRepository.findByTechnician_IdAndPlatform(technicianId, platform)
                    .orElse(null);
        }
        if (entity == null) {
            entity = technicianDeviceTokenRepository.findByDeviceToken(token).orElse(null);
        }
        if (entity == null) {
            entity = TechnicianDeviceToken.builder()
                    .technician(technician)
                    .deviceToken(token)
                    .platform(platform)
                    .build();
        } else {
            entity.setTechnician(technician);
            entity.setDeviceToken(token);
            entity.setPlatform(platform);
        }
        technicianDeviceTokenRepository.save(entity);
    }

    private String defaultName(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private String generateUniqueBadgeNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 50; i++) {
            int rand = ThreadLocalRandom.current().nextInt(0, 100000);
            String candidate = String.format("BDG-%s-%05d", date, rand);
            if (!technicianRepository.existsByBadgeNumberIgnoreCaseAndIsDeletedFalse(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate badge number");
    }

    private String generateUniqueTechnicianId() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 50; i++) {
            int rand = ThreadLocalRandom.current().nextInt(0, 10000);
            String candidate = String.format("TECH-%s-%04d", date, rand);
            if (!technicianRepository.existsByTechnicianIdIgnoreCaseAndIsDeletedFalse(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate technicianId");
    }

    private Role resolveOrCreateTechnicianRole() {
        Role configured = roleRepository.findFirstByTechnicianRoleTrueAndActiveTrue().orElse(null);
        if (configured != null) {
            return configured;
        }

        Role byName = roleRepository.findFirstByNameIgnoreCaseAndCompanyIdIsNullOrderByIdAsc(TECHNICIAN_ROLE_NAME)
                .or(() -> roleRepository.findFirstByNameIgnoreCaseAndActiveTrueOrderByIdAsc(TECHNICIAN_ROLE_NAME))
                .orElse(null);
        if (byName != null) {
            boolean changed = false;
            if (!byName.isActive()) {
                byName.setActive(true);
                changed = true;
            }
            if (!byName.isTechnicianRole()) {
                byName.setTechnicianRole(true);
                changed = true;
            }
            return changed ? roleRepository.save(byName) : byName;
        }

        return roleRepository.save(Role.builder()
                .companyId(null)
                .name(TECHNICIAN_ROLE_NAME)
                .description("Auto-created technician role for TM user login")
                .active(true)
                .technicianRole(true)
                .build());
    }

    private record TmUserLoginRow(
            String firstName,
            String lastName,
            String email,
            String passwordHash,
            boolean active
    ) {
    }
}
