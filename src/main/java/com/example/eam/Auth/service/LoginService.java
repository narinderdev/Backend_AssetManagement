package com.example.eam.Auth.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.eam.Auth.dto.LoginDto;
import com.example.eam.Auth.dto.LoginResponseDto;
import com.example.eam.Auth.dto.MfaLoginDto;
import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.User.entity.PasswordPolicy;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.PasswordPolicyRepository;
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
    private final MfaService mfaService;

    @Value("${app.mfa.token-expiration-ms:300000}")
    private long mfaTokenExpirationMs;


    private static final int DEFAULT_PASSWORD_EXPIRY_DAYS = 90;
    private static final int PASSWORD_EXPIRY_WARNING_DAYS = 7;

    @Transactional
    public LoginResponseDto login(LoginDto dto) {
        String email = dto.getEmail().trim().toLowerCase();

        Users user = usersRepository
                .findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

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
            return new LoginResponseDto(true, mfaToken);
        }

        return buildLoginResponse(user, daysUntilPasswordExpiry, dto.getDeviceToken(), dto.getDevicePlatform());
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
        return buildLoginResponse(user, daysUntilPasswordExpiry, deviceToken, devicePlatform);
    }

    private LoginResponseDto buildLoginResponse(Users user, Integer daysUntilPasswordExpiry, String deviceToken, String devicePlatform) {
        List<String> roles = user.getUserRoles()
                .stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        boolean hasTechnicianRole = user.getUserRoles()
                .stream()
                .anyMatch(ur -> ur.getRole() != null && ur.getRole().isTechnicianRole());
        Long technicianId = hasTechnicianRole ? ensureTechnicianProfile(user) : null;
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
                false
        );
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password expired");
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
}
