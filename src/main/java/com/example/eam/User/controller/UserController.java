package com.example.eam.User.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.eam.Common.ApiResponse;
import com.example.eam.User.dto.ChangePasswordDto;
import com.example.eam.User.dto.ForgotPasswordDto;
import com.example.eam.User.dto.UserCreateDto;
import com.example.eam.User.entity.Users;
import com.example.eam.User.service.UserService;

@RestController
@RequestMapping("/users")

public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Users>> createUser(
        @Valid @RequestBody UserCreateDto dto
        ) {
            Users user = userService.register(dto);
            ApiResponse<Users> response = ApiResponse.successResponse(201, "User Created Successfully", user);
            return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<com.example.eam.User.dto.UserSummaryDto>>> listUsers() {
        var users = userService.listUsers();
        ApiResponse<java.util.List<com.example.eam.User.dto.UserSummaryDto>> response =
                ApiResponse.successResponse(200, "Users fetched successfully", users);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordDto dto
    ) {
        userService.changePassword(dto.getEmail(), dto);
        ApiResponse<Void> response = ApiResponse.successResponse(200, "Password updated successfully", null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordDto dto
    ) {
        userService.forgotPassword(dto);
        ApiResponse<Void> response = ApiResponse.successResponse(200, "Password reset successfully", null);
        return ResponseEntity.ok(response);
    }
}
