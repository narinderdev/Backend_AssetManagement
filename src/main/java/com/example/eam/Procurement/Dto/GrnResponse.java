package com.example.eam.Procurement.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrnResponse {

    private Long id;
    private String grnNumber;
    private Long poId;
    private Long vendorId;
    private String vendorName;
    private String receivedByUserId;
    private Instant receivedAtUtc;
    private String dayKeyUtc;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private List<GrnLineResponse> lines;
}
