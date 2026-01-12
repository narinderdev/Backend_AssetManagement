package com.example.eam.Maintenance.Preventive.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChecklistItemDto {
    @NotBlank
    private String itemText;
    @NotNull
    private Boolean required;
    private Integer sortOrder;
}
