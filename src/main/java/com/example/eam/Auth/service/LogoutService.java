package com.example.eam.Auth.service;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header is missing or invalid");
        }

        String token = authorizationHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        Claims claims = jwtService.parseClaims(token);
        Date expiry = claims.getExpiration();

        if (expiry == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token expiration");
        }

        tokenBlacklistService.blacklistToken(token, expiry.toInstant());
    }
}
