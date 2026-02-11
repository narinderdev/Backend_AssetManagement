package com.example.eam.User.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserUpdateDto {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String lastName;

    @Email(message = "Please provide a valid email address")
    private String email;

    @Size(min = 12, message = "Password must be at least 12 characters long")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must include at least one uppercase letter")
    @Pattern(regexp = ".*[a-z].*", message = "Password must include at least one lowercase letter")
    @Pattern(regexp = ".*[^A-Za-z0-9\\s].*", message = "Password must include at least one special character")
    private String password;
}
