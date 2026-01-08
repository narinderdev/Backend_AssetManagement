package com.example.eam.Auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private LogoutService logoutService;

    @Test
    void logout_missingHeader_throwsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> logoutService.logout(null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void logout_invalidToken_throwsUnauthorized() {
        when(jwtService.isTokenValid("bad")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> logoutService.logout("Bearer bad"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(tokenBlacklistService, never()).blacklistToken(anyString(), any());
    }

    @Test
    void logout_validToken_blacklists() {
        String token = "good-token";
        Claims claims = mock(Claims.class);

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.parseClaims(token)).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));

        logoutService.logout("Bearer " + token);

        verify(tokenBlacklistService).blacklistToken(eq(token), any());
    }
}
