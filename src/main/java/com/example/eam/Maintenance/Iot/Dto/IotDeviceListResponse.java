package com.example.eam.Maintenance.Iot.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IotDeviceListResponse {
    private List<IotDeviceResponse> devices;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
