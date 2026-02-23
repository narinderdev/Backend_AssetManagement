package com.example.eam.Auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaEmailRequestDto {

    @Email(message = "email must be valid")
    @NotBlank(message = "email is required")
    private String email;
}
