package com.example.eam.Procurement.Dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMaterialRequisitionRequest {

    private String requestedByUserId;

    private LocalDate neededByDate;

    private String notes;

    private String shipToType; // WAREHOUSE or WORK_SITE
    private Long shipToWarehouseId;
    private Long shipToWorkOrderId;

    @Valid
    private List<MaterialRequisitionLineRequest> lines;
}
