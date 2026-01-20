package com.example.eam.Procurement.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGrnRequest {

    private Long poId;

    @NotBlank
    private String receivedByUserId;

    private String notes;

    @NotEmpty
    @Valid
    private List<CreateGrnLineRequest> lines;
}
