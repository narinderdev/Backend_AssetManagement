package com.example.eam.WorkRequestType.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkRequestTypeCreateRequest {

    @NotBlank
    private String code;

    private String description;
}
