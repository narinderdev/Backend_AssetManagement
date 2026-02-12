package com.example.eam.Auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.eam.Auth.dto.MfaCodeDto;
import com.example.eam.Auth.dto.MfaSetupResponseDto;
import com.example.eam.Auth.service.MfaService;
import com.example.eam.Common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;

    @GetMapping("/setup")
    public ResponseEntity<ApiResponse<MfaSetupResponseDto>> setup() {
        String email = currentUserEmail();
        MfaSetupResponseDto response = mfaService.setup(email);
        ApiResponse<MfaSetupResponseDto> body = ApiResponse.successResponse(
                200,
                "MFA setup initialized",
                response
        );
        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify-setup")
    public ResponseEntity<ApiResponse<Void>> verifySetup(@Valid @RequestBody MfaCodeDto dto) {
        String email = currentUserEmail();
        mfaService.verifySetup(email, dto.getCode());
        ApiResponse<Void> body = ApiResponse.successResponse(
                200,
                "MFA enabled successfully",
                null
        );
        return ResponseEntity.ok(body);
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Void>> disable(@Valid @RequestBody MfaCodeDto dto) {
        String email = currentUserEmail();
        mfaService.disable(email, dto.getCode());
        ApiResponse<Void> body = ApiResponse.successResponse(
                200,
                "MFA disabled successfully",
                null
        );
        return ResponseEntity.ok(body);
    }

    private String currentUserEmail() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? String.valueOf(auth.getPrincipal()) : null;
    }
}
