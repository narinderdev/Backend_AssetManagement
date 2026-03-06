package com.example.eam.Procurement.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequisitionLinkedGrnResponse {
    private Long id;
    private String grnNumber;
}
