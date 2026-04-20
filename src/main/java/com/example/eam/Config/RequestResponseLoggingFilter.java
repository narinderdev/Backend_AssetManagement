package com.example.eam.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Slf4j
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final int REQUEST_CACHE_LIMIT = 1024 * 1024; // 1MB

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // skip swagger/docs/actuator/static
        return path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/webjars")
                || path.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ✅ use the 2-arg ctor in Spring 7
        ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);

        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            logRequest(requestWrapper);
            logResponse(responseWrapper, duration);

            // important!
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullUrl = uri + (query != null ? "?" + query : "");

        String contentType = request.getContentType();
        boolean isMultipart = contentType != null && contentType.startsWith("multipart/");

        StringBuilder sb = new StringBuilder();
        sb.append("\n--- [HTTP REQUEST] --------------------------------\n");
        sb.append(method).append(" ").append(fullUrl).append("\n");

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            if (isSensitiveHeader(name)) {
                value = "***";
            }
            sb.append("Header: ").append(name).append(" = ").append(value).append("\n");
        }

        if (!isMultipart) {
            String body = getRequestBody(request);
            if (!body.isEmpty()) {
                sb.append("Body: ").append(maskSensitive(body)).append("\n");
            }
        } else {
            sb.append("Body: [multipart content omitted]\n");
        }

        sb.append("---------------------------------------------------");
        log.info(sb.toString());
    }

    private void logResponse(ContentCachingResponseWrapper response, long durationMs) {
        int status = response.getStatus();
        String contentType = response.getContentType();
        boolean isBinary = contentType != null && contentType.startsWith("application/octet-stream");

        StringBuilder sb = new StringBuilder();
        sb.append("\n--- [HTTP RESPONSE] -------------------------------\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Duration: ").append(durationMs).append(" ms\n");

        for (String name : response.getHeaderNames()) {
            sb.append("Header: ").append(name)
              .append(" = ").append(response.getHeader(name))
              .append("\n");
        }

        if (!isBinary) {
            String body = getResponseBody(response);
            if (!body.isEmpty()) {
                sb.append("Body: ").append(maskSensitive(body)).append("\n");
            }
        } else {
            sb.append("Body: [binary content omitted]\n");
        }

        sb.append("---------------------------------------------------");
        log.info(sb.toString());
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length == 0) return "";
        return new String(buf, StandardCharsets.UTF_8);
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length == 0) return "";
        return new String(buf, StandardCharsets.UTF_8);
    }

    private String maskSensitive(String body) {
        if (body == null || body.isBlank()) return body;
        return body
                .replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
                .replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"")
                .replaceAll("\"mfaToken\"\\s*:\\s*\"[^\"]*\"", "\"mfaToken\":\"***\"")
                .replaceAll("\"mfa_token\"\\s*:\\s*\"[^\"]*\"", "\"mfa_token\":\"***\"")
                .replaceAll("\"code\"\\s*:\\s*\"\\d{6}\"", "\"code\":\"***\"")
                .replaceAll("\"otp\"\\s*:\\s*\"\\d{4,8}\"", "\"otp\":\"***\"")
                .replaceAll("\"deviceSecret\"\\s*:\\s*\"[^\"]*\"", "\"deviceSecret\":\"***\"")
                .replaceAll("\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"");
    }

    private boolean isSensitiveHeader(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.trim().toLowerCase();
        return normalized.equals("authorization")
                || normalized.equals("x-iot-signature")
                || normalized.equals("x-iot-device-uid")
                || normalized.equals("x-iot-nonce");
    }
}

