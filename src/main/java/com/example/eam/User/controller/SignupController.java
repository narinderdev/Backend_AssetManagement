package com.example.eam.User.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.eam.Common.ApiResponse;
import com.example.eam.User.dto.SignupVerifyDto;
import com.example.eam.User.dto.UserCreateDto;
import com.example.eam.User.entity.Users;
import com.example.eam.User.service.SignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    /* =========================
       SEND OTP
       ========================= */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> signup(
            @Valid @RequestBody UserCreateDto dto
    ) {
        signupService.startSignup(dto);

        return ResponseEntity.accepted().body(
                ApiResponse.successResponse(
                        202,
                        "OTP sent to email. Please verify.",
                        null
                )
        );
    }

    /* =========================
       VERIFY OTP & CREATE USER
       ========================= */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Users>> verifyOtp(
            @RequestBody SignupVerifyDto dto
    ) {
        Users user = signupService.verifyOtpAndCreateUser(dto);

        return ResponseEntity.ok(
                ApiResponse.successResponse(
                        201,
                        "User created successfully",
                        user
                )
        );
    }
}

