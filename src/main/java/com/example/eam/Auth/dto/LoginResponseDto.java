package com.example.eam.Auth.dto;

import com.example.eam.User.entity.Users;

public class LoginResponseDto {
    private String token;
    private Users user;
    private Long technicianId;

    public LoginResponseDto(String token, Users user, Long technicianId) {
        this.token = token;
        this.user = user;
        this.technicianId = technicianId;
    }

    public String getToken() { return token; }
    public Users getUser() { return user; }
    public Long getTechnicianId() { return technicianId; }
}
