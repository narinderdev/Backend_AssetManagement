package com.example.eam.User.dto;

import com.example.eam.User.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserRoleAssignmentResponse {

    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private UserStatus status;
    private List<RoleAssignment> roles;
    private Long technicianId;

    @Data
    @Builder
    public static class RoleAssignment {
        private Long id;
        private String name;
    }
}
