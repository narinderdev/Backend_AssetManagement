package com.example.eam.User.controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.User.dto.InviteUserRequest;
import com.example.eam.User.dto.SetPasswordDto;
import com.example.eam.User.entity.Users;
import com.example.eam.User.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class InviteController {

    private final InvitationService invitationService;

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Users>> inviteUser(
            @Valid @RequestBody InviteUserRequest request
    ) {
        Users invited = invitationService.inviteUser(request);
        ApiResponse<Users> body = ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Invitation sent successfully",
                invited
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/accept")
    public ResponseEntity<Void> acceptInvite(@RequestParam String email) {
        invitationService.validateInvite(email);
        String redirectUrl = invitationService.getSetPasswordRedirectUrl(email);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @Valid @RequestBody SetPasswordDto dto
    ) {
        invitationService.setPassword(dto);
        ApiResponse<Void> body = ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Password set successfully",
                null
        );
        return ResponseEntity.ok(body);
    }
}
