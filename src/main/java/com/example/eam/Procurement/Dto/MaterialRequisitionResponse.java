package com.example.eam.Procurement.Dto;

import com.example.eam.Procurement.Enum.MaterialRequisitionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequisitionResponse {

    private Long id;
    private String mrNumber;
    private String requestedByUserId;
    private MaterialRequisitionStatus status;
    private LocalDate neededByDate;
    private String notes;
    private String shipToType;
    private Long shipToWarehouseId;
    private Long shipToWorkOrderId;
    private String approvedByUserId;
    private Instant approvedAt;
    private String rejectedByUserId;
    private Instant rejectedAt;
    private String rejectionReason;
    private Instant createdAt;
    private Instant updatedAt;
    private List<MaterialRequisitionLineResponse> lines;
}
