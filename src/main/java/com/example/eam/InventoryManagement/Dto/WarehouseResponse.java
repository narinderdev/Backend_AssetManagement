package com.example.eam.InventoryManagement.Dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonPropertyOrder({"id","name","address","zoneAisle","rackShelf","binCode","binDescription","active","createdAt","updatedAt"})
public class WarehouseResponse {
    private Long id;
    private String name;
    private String address;
    private String zoneAisle;
    private String rackShelf;
    private String binCode;
    private String binDescription;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
