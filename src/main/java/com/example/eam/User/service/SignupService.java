package com.example.eam.User.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.eam.Common.EmailService;
import com.example.eam.Roles.Entity.Role;
import com.example.eam.Roles.Repository.RoleRepository;
import com.example.eam.User.dto.SignupVerifyDto;
import com.example.eam.User.dto.UserCreateDto;
import com.example.eam.User.entity.PendingUserSignup;
import com.example.eam.User.entity.UserRole;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.PendingUserSignupRepository;
import com.example.eam.User.repository.UserRoleRepository;
import com.example.eam.User.repository.UsersRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final PendingUserSignupRepository pendingRepo;
    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 5;

    /* =========================
       STEP 1: START SIGNUP
       ========================= */
    @Transactional
    public void startSignup(UserCreateDto dto) {

        String email = dto.getEmail().trim().toLowerCase();

        // Check existing user
        if (usersRepository.findByEmailAndDeletedFalse(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already exists with this email"
            );
        }

        // Check pending signup (NO lambda)
        Optional<PendingUserSignup> existingOpt = pendingRepo.findByEmail(email);

        if (existingOpt.isPresent()) {
            PendingUserSignup existing = existingOpt.get();

            if (existing.getExpiresAt().isAfter(Instant.now())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "OTP already sent. Please verify your email."
                );
            }

            // OTP expired cleanup
            pendingRepo.delete(existing);
            pendingRepo.flush(); // Required for SQL Server
        }

        // Create new pending signup
        String otp = generateOtp();

        PendingUserSignup pending = PendingUserSignup.builder()
                .email(email)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .otp(otp)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(OTP_EXPIRY_MINUTES)))
                .build();

        pendingRepo.save(pending);

        // Send email (outside DB logic)
        try {
            String body = """
                <p>Your OTP for signup is:</p>
                <h2>%s</h2>
                <p>Valid for %d minutes.</p>
            """.formatted(otp, OTP_EXPIRY_MINUTES);

            emailService.sendWithAttachment(
                    email,
                    "Verify your email",
                    body,
                    null
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to send OTP email. Please try again."
            );
        }
    }

    /* =========================
       STEP 2: VERIFY OTP & CREATE USER
       ========================= */
    @Transactional
    public Users verifyOtpAndCreateUser(SignupVerifyDto dto) {

        String email = dto.getEmail().trim().toLowerCase();

        PendingUserSignup pending = pendingRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Signup not found"
                ));

        if (pending.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "OTP expired"
            );
        }

        if (!pending.getOtp().equals(dto.getOtp())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid OTP"
            );
        }

        Role role = roleRepository.findByNameIgnoreCase("Admin")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Admin role not found"
                ));

        Users user = new Users();
        user.setFirstName(pending.getFirstName());
        user.setLastName(pending.getLastName());
        user.setEmail(pending.getEmail());
        user.setPassword(pending.getPasswordHash());
        user.setUpdatedAt(Instant.now());
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);

        Users savedUser = usersRepository.save(user);

        userRoleRepository.save(
                UserRole.builder()
                        .user(savedUser)
                        .role(role)
                        .build()
        );

        // cleanup
        pendingRepo.delete(pending);

        return savedUser;
    }

    private String generateOtp() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }
}
