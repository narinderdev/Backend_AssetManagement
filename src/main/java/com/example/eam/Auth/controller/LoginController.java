package com.example.eam.Auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.eam.Auth.dto.LoginDto;
import com.example.eam.Auth.dto.LoginResponseDto;
import com.example.eam.Auth.service.LoginService;
import com.example.eam.Common.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class LoginController {
    private final LoginService loginService;

    public LoginController(LoginService loginService){
        this.loginService = loginService;
    }
    

    @PostMapping()
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginDto dto) {
        LoginResponseDto user = loginService.login(dto);

        ApiResponse<LoginResponseDto> body = ApiResponse.successResponse(
            201, 
            "Login Successfully", 
            user
        );
        return ResponseEntity.status(200).body(body);
    }
}
