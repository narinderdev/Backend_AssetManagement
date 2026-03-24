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
import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Roles.Entity.Role;
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
    private final MfaService mfaService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final Map<String, JdbcTemplate> jdbcTemplates;

    @Value("${app.mfa.token-expiration-ms:300000}")
    private long mfaTokenExpirationMs;


    private static final int DEFAULT_PASSWORD_EXPIRY_DAYS = 90;
    private static final int PASSWORD_EXPIRY_WARNING_DAYS = 7;
    private static final String TECHNICIAN_ROLE_NAME = "Technician";
    private static final String TM_TECHNICIAN_ID_PREFIX = "TM-";
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
        if (!mfaService.verifyActiveCode(user, dto.getCode())) {
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

        List<LoginResponseDto.CompanySummary> companies = userCompanyRepository
                .findByUser_IdAndCompany_ActiveTrue(user.getId())
                .stream()
                .map(UserCompany::getCompany)
                .filter(Objects::nonNull)
                .map(c -> new LoginResponseDto.CompanySummary(
                        c.getId(),
                        c.getCompanyLegalName(),
                        c.getCompanyTradeName(),
                        c.getCompanyNumber(),
                        c.getAddress(),
                        c.getCity(),
                        c.getCountry(),
                        c.getPostalCode()
                ))
                .toList();

        Boolean isCompanySetup = isAdmin ? !companies.isEmpty() : null;

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

        String token = jwtService.generateToken(
                user.getEmail(),
                Map.of(
                        "userId", user.getId(),
                        "email", user.getEmail(),
                        "roles", roles
                )
        );
        return new LoginResponseDto(
                token,
                user,
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
