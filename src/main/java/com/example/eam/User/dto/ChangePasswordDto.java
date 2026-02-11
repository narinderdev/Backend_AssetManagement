package com.example.eam.User.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDto {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 12, message = "Password must be at least 12 characters long")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must include at least one uppercase letter")
    @Pattern(regexp = ".*[a-z].*", message = "Password must include at least one lowercase letter")
    @Pattern(regexp = ".*[^A-Za-z0-9\\s].*", message = "Password must include at least one special character")
    private String newPassword;
}
