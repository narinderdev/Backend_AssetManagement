package com.example.eam.Technician.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@Value
@Builder
public class TimeWindowDto {
    LocalTime start;
    LocalTime end;
}
