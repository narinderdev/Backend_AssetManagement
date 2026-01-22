package com.example.eam.WorkOrder.Dto;

import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.WorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkOrderCreateRequest {

    // Asset optional (location-only WO allowed)
    private Long assetId;

    // If assetId is null, location is required
    private String location;

    @NotNull
    private WorkType workType;

    @NotNull
    private PriorityLevel priority;

    @NotBlank
    private String woTitle;

    @NotBlank
    private String descriptionScope;

    @NotNull
    private LocalDate targetCompletionDate;

    // optional image upload URL
    private String attachmentUrl;

    @NotBlank
    private String workRequestTypeCode;
}
