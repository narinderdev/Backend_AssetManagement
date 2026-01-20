package com.example.eam.Auth.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.eam.Auth.dto.LoginDto;
import com.example.eam.Auth.dto.LoginResponseDto;
import com.example.eam.Enum.TechnicianStatus;
import com.example.eam.Enum.TechnicianType;
import com.example.eam.Technician.Entity.Technician;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.UsersRepository;
import com.example.eam.Enum.DevicePlatform;
import com.example.eam.TechnicianTeam.Entity.TechnicianTeamMember;
import com.example.eam.TechnicianTeam.Repository.TechnicianTeamMemberRepository;
import com.example.eam.Technician.Repository.TechnicianDeviceTokenRepository;
import com.example.eam.Technician.Entity.TechnicianDeviceToken;

import org.springframework.security.crypto.password.PasswordEncoder;

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

        List<String> roles = user.getUserRoles()
                .stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        boolean hasTechnicianRole = user.getUserRoles()
                .stream()
                .anyMatch(ur -> ur.getRole() != null && ur.getRole().isTechnicianRole());
        Long technicianId = hasTechnicianRole ? ensureTechnicianProfile(user) : null;
        if (technicianId != null && dto.getDeviceToken() != null && !dto.getDeviceToken().trim().isEmpty()) {
            registerDeviceToken(technicianId, dto.getDeviceToken(), dto.getDevicePlatform());
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

        return new LoginResponseDto(token, user, technicianId, isTechnician, isTeamLeader, leaderTeams);
    }

    private Long ensureTechnicianProfile(Users user) {
        String normalizedEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : null;
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User email is required to resolve technician profile");
        }

        return technicianRepository.findByEmailIgnoreCase(normalizedEmail)
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

        Technician technician = technicianRepository.findById(technicianId)
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
