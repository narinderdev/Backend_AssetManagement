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
import com.example.eam.Auth.dto.MfaLoginDto;
import com.example.eam.Auth.service.LogoutService;
import com.example.eam.Auth.service.LoginService;
import com.example.eam.Common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final LogoutService logoutService;
    

    @PostMapping()
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginDto dto) {
        LoginResponseDto user = loginService.login(dto);

        String message = Boolean.TRUE.equals(user.getMfaRequired())
                ? "MFA required"
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
