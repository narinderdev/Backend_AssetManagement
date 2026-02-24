package com.example.eam.Auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.eam.Auth.dto.LoginDto;
import com.example.eam.Auth.dto.LoginResponseDto;
import com.example.eam.Auth.dto.MfaEmailRequestDto;
import com.example.eam.Auth.dto.MfaEmailVerifyDto;
import com.example.eam.Auth.dto.MfaLoginDto;
import com.example.eam.Auth.service.LogoutService;
import com.example.eam.Auth.service.LoginService;
import com.example.eam.Auth.service.MfaService;
import com.example.eam.Common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final LogoutService logoutService;
    private final MfaService mfaService;
    

    @PostMapping()
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginDto dto) {
        LoginResponseDto user = loginService.login(dto);

        String message = Boolean.TRUE.equals(user.getMfaRequired())
                // ? "MFA required" 
                ?"OTP Sent on Email"
                : "Login Successfully";
        ApiResponse<LoginResponseDto> body = ApiResponse.successResponse(
            201, 
            message, 
            user
        );
        return ResponseEntity.status(200).body(body);
    }

    @PostMapping("/login/mfa")
    public ResponseEntity<ApiResponse<LoginResponseDto>> loginWithMfa(@Valid @RequestBody MfaLoginDto dto) {
        LoginResponseDto user = loginService.loginWithMfa(dto);
        ApiResponse<LoginResponseDto> body = ApiResponse.successResponse(
            200,
            "Login Successfully",
            user
        );
        return ResponseEntity.ok(body);
    }

    @PostMapping("/mfa/email/send")
    public ResponseEntity<ApiResponse<Void>> sendMfaEmailOtp(@Valid @RequestBody MfaEmailRequestDto dto) {
        mfaService.sendEmailOtp(dto.getEmail());
        ApiResponse<Void> body = ApiResponse.successResponse(
                200,
                "Email OTP sent successfully",
                null
        );
        return ResponseEntity.ok(body);
    }

    @PostMapping("/mfa/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyMfaEmailOtp(@Valid @RequestBody MfaEmailVerifyDto dto) {
        mfaService.verifyEmailOtp(dto.getEmail(), dto.getCode());
        ApiResponse<Void> body = ApiResponse.successResponse(
                200,
                "Email OTP verified successfully",
                null
        );
        return ResponseEntity.ok(body);
    }

    private String currentUserEmail() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? String.valueOf(auth.getPrincipal()) : null;
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        logoutService.logout(authorizationHeader);

        ApiResponse<Void> body = ApiResponse.successResponse(
            HttpStatus.OK.value(),
            "Logout Successfully",
            null
        );

        return ResponseEntity.ok(body);
    }
}
