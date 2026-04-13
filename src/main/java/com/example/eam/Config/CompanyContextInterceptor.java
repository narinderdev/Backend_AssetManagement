package com.example.eam.Config;

import com.example.eam.Common.ApiResponse;
import com.example.eam.Common.CompanyContextHolder;
import com.example.eam.Technician.Repository.TechnicianRepository;
import com.example.eam.User.repository.UserCompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CompanyContextInterceptor implements HandlerInterceptor {

    private static final String VOICE_AI_INTAKE_PATH = "/api/voice-ai/intake";
    private static final long VOICE_AI_DEFAULT_COMPANY_ID = 2L;

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/api/companies",
            "/api/permissions",
            "/api/security-dashboard",
            "/api/mfa"
    );

    private final ObjectMapper objectMapper;
    private final UserCompanyRepository userCompanyRepository;
    private final TechnicianRepository technicianRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/") || isExcluded(path)) {
            return true;
        }

        if (isVoiceAiIntakePath(path)) {
            CompanyContextHolder.setCompanyId(VOICE_AI_DEFAULT_COMPANY_ID);
            return true;
        }

        String rawCompanyId = request.getParameter("companyId");
        if (rawCompanyId == null || rawCompanyId.isBlank()) {
            writeBadRequest(response, "companyId query parameter is required");
            return false;
        }

        Long companyId;
        try {
            companyId = Long.parseLong(rawCompanyId.trim());
        } catch (NumberFormatException ex) {
            writeBadRequest(response, "companyId must be a valid number");
            return false;
        }

        if (companyId <= 0) {
            writeBadRequest(response, "companyId must be greater than 0");
            return false;
        }

        String userEmail = resolveCurrentUserEmail();
        if (userEmail == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized");
            return false;
        }

        boolean allowed = userCompanyRepository.existsActiveMapping(
                userEmail,
                companyId
        );
        if (!allowed) {
            allowed = technicianRepository.existsByEmailIgnoreCaseAndIsDeletedFalseAndCompanyId(userEmail, companyId);
        }
        if (!allowed) {
            writeError(response, HttpStatus.FORBIDDEN, "You are not assigned to this company");
            return false;
        }

        CompanyContextHolder.setCompanyId(companyId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CompanyContextHolder.clear();
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isVoiceAiIntakePath(String path) {
        return VOICE_AI_INTAKE_PATH.equals(path) || (VOICE_AI_INTAKE_PATH + "/").equals(path);
    }

    private String resolveCurrentUserEmail() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }
            Object principal = auth.getPrincipal();
            if (principal == null) {
                return null;
            }
            String email = String.valueOf(principal).trim();
            return email.isBlank() ? null : email;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeBadRequest(HttpServletResponse response, String message) throws Exception {
        writeError(response, HttpStatus.BAD_REQUEST, message);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.errorResponse(status.value(), message)));
    }
}
