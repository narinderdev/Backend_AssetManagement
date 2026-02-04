package com.example.eam.Technician.Dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TechnicianHolidayListResponse {
    List<TechnicianHolidayResponse> holidays;
}
