package com.example.eam.Auth.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Instant expiresAt) {
        if (token == null || expiresAt == null) {
            return;
        }
        blacklistedTokens.put(token, expiresAt);
    }

    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }

        Instant expiry = blacklistedTokens.get(token);
        if (expiry == null) {
            return false;
        }

        // Drop expired blacklist entries
        if (expiry.isBefore(Instant.now())) {
            blacklistedTokens.remove(token);
            return false;
        }

        return true;
    }
}
