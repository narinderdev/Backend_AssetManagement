package com.example.eam.Maintenance.Preventive.Dto;

import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.PreventiveScheduleType;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.TimeFrequencyUnit;
import com.example.eam.Enum.WorkType;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PreventivePlanPatchRequest {
    private Long assetId;
    private String location;
    private String title;
    private WorkType workType;
    private PriorityLevel priority;
    private PreventiveScheduleType scheduleType;
    private Integer leadTimeDays;
    private LocalDate startDate;
    private TimeFrequencyUnit intervalUnit;
    private Integer intervalValue;
    private MeterType meterType;
    private Integer meterIntervalValue;
    private Integer currentMeterReading;
    private Boolean active;
    private List<ChecklistItemDto> checklistItems;
}
