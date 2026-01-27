package com.example.eam.InventoryManagement.Dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehousePatchRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 512)
    private String address;

    @Size(max = 128)
    private String zoneAisle;

    @Size(max = 128)
    private String rackShelf;

    @Size(max = 128)
    private String binCode;

    @Size(max = 512)
    private String binDescription;

    private Boolean active;
}
