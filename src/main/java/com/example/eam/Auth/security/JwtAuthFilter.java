package com.example.eam.Auth.security;

import com.example.eam.Auth.service.JwtService;
import com.example.eam.Auth.service.TokenBlacklistService;
import com.example.eam.Common.ApiResponse;
import com.example.eam.User.entity.PasswordPolicy;
import com.example.eam.User.entity.Users;
import com.example.eam.User.repository.PasswordPolicyRepository;
import com.example.eam.User.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UsersRepository usersRepository;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_PASSWORD_EXPIRY_DAYS = 90;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (tokenBlacklistService.isBlacklisted(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.parseClaims(token);
        String tokenType = claims.get("type", String.class);
        if (tokenType != null && !tokenType.isBlank() && !"access".equals(tokenType)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = claims.getSubject();

        // ✅ SAFE extraction of roles
        Users user = usersRepository.findByEmailAndDeletedFalse(email).orElse(null);
        if (user != null && !isPasswordExpiryBypassed(request) && isPasswordExpired(user)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            ApiResponse<Void> body = ApiResponse.errorResponse(401, "your password is expired");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        Object rolesObj = claims.get("roles");

        List<String> roles = rolesObj instanceof List
                ? ((List<?>) rolesObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        // ✅ Convert roles to Spring authorities
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        email, null, authorities
                );

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    private boolean isPasswordExpiryBypassed(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return false;
        // Allow password change / reset endpoints even when password is expired
        return path.contains("/users/change-password")
                || path.contains("/users/set-password")
                || path.contains("/users/accept");
    }

    private boolean isPasswordExpired(Users user) {
        int expiryDays = passwordPolicyRepository.findTopByOrderByIdAsc()
                .map(PasswordPolicy::getPasswordExpiryDays)
                .filter(days -> days != null && days > 0)
                .orElse(DEFAULT_PASSWORD_EXPIRY_DAYS);
        LocalDate baseDate = resolvePasswordChangedAt(user);
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), baseDate.plusDays(expiryDays));
        return daysRemaining < 0;
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
}
