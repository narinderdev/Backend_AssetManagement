package com.example.eam.Asset.Dto;


import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetWarrantyLifecycleDto {

    private LocalDate commissioningDate;
    private LocalDate warrantyStart;
    private LocalDate warrantyEnd;           // from duplicate Warranty Start
    private String warrantyProvider;
    private String serviceContract;
    private LocalDate plannedReplacementDate;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextPlannedMaintenance;
}

