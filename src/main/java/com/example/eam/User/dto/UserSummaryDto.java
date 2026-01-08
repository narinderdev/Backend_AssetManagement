package com.example.eam.User.dto;

import java.util.List;

import com.example.eam.User.entity.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSummaryDto {
    private Long id;
    private String name;
    private String email;
    private UserStatus status;
    private List<String> roles;
}
