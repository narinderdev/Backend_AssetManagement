package com.example.eam.Auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MfaSetupResponseDto {
    private String secret;
    private String qrCodeImage;
}
