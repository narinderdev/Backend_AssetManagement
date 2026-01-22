package com.example.eam.WorkRequestType.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkRequestTypeResponse {

    private Long id;
    private String code;
    private String description;
}
