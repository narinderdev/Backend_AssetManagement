package com.example.eam.Auth.service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.eam.Auth.dto.MfaSetupResponseDto;
import com.example.eam.Common.EmailService;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.UsersRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final UsersRepository usersRepository;
    private final TotpService totpService;
    private final MfaCryptoService cryptoService;
    private final MfaRateLimitService rateLimitService;
    private final EmailService emailService;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.mfa.app-name:EAM}")
    private String appName;

    @Value("${app.mfa.qr-size:240}")
    private int qrSize;

    @Value("${app.mfa.email-otp-expiry-minutes:5}")
    private int emailOtpExpiryMinutes;

    public MfaSetupResponseDto setup(String email) {
        Users user = requireUser(email);
        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is already enabled");
        }
        String secret = totpService.generateSecret();
        user.setMfaSecretTemp(cryptoService.encrypt(secret));
        usersRepository.save(user);

        String otpAuthUrl = buildOtpAuthUrl(appName, secret);
        String qrCodeImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(generateQrPng(otpAuthUrl));
        return new MfaSetupResponseDto(secret, qrCodeImage);
    }

    public void verifySetup(String email, String code) {
        Users user = requireUser(email);
        rateLimitService.checkOrThrow(rateLimitKey("setup", user.getId()));

        String encryptedSecret = user.getMfaSecretTemp();
        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA setup not initialized");
        }
        String secret = cryptoService.decrypt(encryptedSecret);
        if (!totpService.verifyCode(secret, code, 1)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        user.setMfaSecret(encryptedSecret);
        user.setMfaSecretTemp(null);
        user.setMfaEnabled(true);
        usersRepository.save(user);
    }

    public void disable(String email, String code) {
        Users user = requireUser(email);
        rateLimitService.checkOrThrow(rateLimitKey("disable", user.getId()));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is not enabled");
        }
        String secret = cryptoService.decrypt(user.getMfaSecret());
        if (!totpService.verifyCode(secret, code, 1)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaSecretTemp(null);
        usersRepository.save(user);
    }

    public void sendEmailOtp(String email) {
        Users user = requireUser(email);
        rateLimitService.checkOrThrow(rateLimitKey("email_send", user.getId()));

        String otp = generateOtp();
        user.setMfaEmailOtp(otp);
        user.setMfaEmailOtpExpiresAt(Instant.now().plus(Duration.ofMinutes(emailOtpExpiryMinutes)));
        user.setMfaEmailVerified(false);
        usersRepository.save(user);

        String html = """
                <p>Your MFA email verification code is:</p>
                <h2>%s</h2>
                <p>This code expires in %d minutes.</p>
                """.formatted(otp, emailOtpExpiryMinutes);
        emailService.sendWithAttachment(
                user.getEmail(),
                "Your MFA email verification code",
                html,
                null
        );
    }

    public void verifyEmailOtp(String email, String code) {
        Users user = requireUser(email);
        rateLimitService.checkOrThrow(rateLimitKey("email_verify", user.getId()));

        String otp = user.getMfaEmailOtp();
        Instant expiresAt = user.getMfaEmailOtpExpiresAt();
        if (otp == null || otp.isBlank() || expiresAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email OTP not requested");
        }
        if (Instant.now().isAfter(expiresAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email OTP expired");
        }
        if (!otp.equals(code)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email OTP");
        }

        user.setMfaEmailVerified(true);
        user.setMfaEmailOtp(null);
        user.setMfaEmailOtpExpiresAt(null);
        usersRepository.save(user);
    }

    public boolean verifyActiveCode(Users user, String code) {
        if (user == null || !user.isMfaEnabled() || user.getMfaSecret() == null) {
            return false;
        }
        String secret = cryptoService.decrypt(user.getMfaSecret());
        return totpService.verifyCode(secret, code, 1);
    }

    public void checkLoginRateLimit(Long userId) {
        rateLimitService.checkOrThrow(rateLimitKey("login", userId));
    }

    public int getDigits() {
        return totpService.getDigits();
    }

    public int getPeriodSeconds() {
        return totpService.getPeriodSeconds();
    }

    private String buildOtpAuthUrl(String appName, String secret) {
        String label = URLEncoder.encode(appName, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&algorithm=SHA1&digits=" + totpService.getDigits()
                + "&period=" + totpService.getPeriodSeconds();
    }

    private byte[] generateQrPng(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, qrSize, qrSize);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate QR code");
        }
    }

    private Users requireUser(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return usersRepository.findByEmailAndDeletedFalse(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String rateLimitKey(String action, Long userId) {
        return "mfa:" + action + ":" + userId;
    }

    private String generateOtp() {
        int value = random.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
