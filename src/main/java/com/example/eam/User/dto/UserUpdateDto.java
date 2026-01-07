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

    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;
}
