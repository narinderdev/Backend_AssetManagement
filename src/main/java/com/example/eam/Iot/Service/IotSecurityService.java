package com.example.eam.Iot.Service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class IotSecurityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long SIGNATURE_WINDOW_SECONDS = 300L;

    public String generateDeviceSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashSecret(String rawSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash secret", ex);
        }
    }

    public boolean isTimestampWithinWindow(long epochSeconds) {
        long now = Instant.now().getEpochSecond();
        return Math.abs(now - epochSeconds) <= SIGNATURE_WINDOW_SECONDS;
    }

    public boolean verifySignature(String deviceUid,
                                   String timestamp,
                                   String nonce,
                                   String rawBody,
                                   String providedSignature,
                                   String storedSecretHash) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        try {
            String canonical = deviceUid + "\n" + timestamp + "\n" + nonce + "\n" + rawBody;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(storedSecretHash.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            byte[] provided = Base64.getDecoder().decode(providedSignature);
            return MessageDigest.isEqual(expected, provided);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean matchesSecret(String rawSecret, String storedSecretHash) {
        if (rawSecret == null || rawSecret.isBlank() || storedSecretHash == null || storedSecretHash.isBlank()) {
            return false;
        }
        String candidate = hashSecret(rawSecret.trim());
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                storedSecretHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}
