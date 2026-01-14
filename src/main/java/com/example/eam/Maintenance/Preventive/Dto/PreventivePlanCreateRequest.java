package com.example.eam.Maintenance.Preventive.Dto;

import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.PreventiveScheduleType;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.TimeFrequencyUnit;
import com.example.eam.Enum.WorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PreventivePlanCreateRequest {
    private Long assetId;
    private String location;

    @NotBlank
    private String title;

    private WorkType workType = WorkType.PREVENTIVE;
    private PriorityLevel priority = PriorityLevel.MEDIUM;

    @NotNull
    private PreventiveScheduleType scheduleType;

    @NotNull
    private Integer leadTimeDays;

    @NotNull
    private LocalDate startDate;

    // Time-based
    private TimeFrequencyUnit intervalUnit;
    @Positive
    private Integer intervalValue;

    // Usage-based
    private MeterType meterType;
    @Positive
    private Integer meterIntervalValue;
    private Integer currentMeterReading;
}
