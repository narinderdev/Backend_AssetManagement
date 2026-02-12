package com.example.eam.Auth.service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.eam.Auth.dto.MfaSetupResponseDto;
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

    @Value("${app.mfa.app-name:EAM}")
    private String appName;

    @Value("${app.mfa.qr-size:240}")
    private int qrSize;

    public MfaSetupResponseDto setup(String email) {
        Users user = requireUser(email);
        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA is already enabled");
        }

        String secret = totpService.generateSecret();
        user.setMfaSecretTemp(cryptoService.encrypt(secret));
        usersRepository.save(user);

        String otpAuthUrl = buildOtpAuthUrl(appName, user.getEmail(), secret);
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

    private String buildOtpAuthUrl(String appName, String email, String secret) {
        String label = URLEncoder.encode(appName, StandardCharsets.UTF_8);
        String issuer = URLEncoder.encode(appName, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
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
}
