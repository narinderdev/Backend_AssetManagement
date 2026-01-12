package com.example.eam.Maintenance.Preventive.Dto;

import com.example.eam.Enum.MeterType;
import com.example.eam.Enum.PreventiveScheduleType;
import com.example.eam.Enum.PriorityLevel;
import com.example.eam.Enum.TimeFrequencyUnit;
import com.example.eam.Enum.WorkType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PreventivePlanResponse {
    private Long id;
    private String planCode;
    private String title;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String location;
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
    private LocalDate nextDueDate;
    private Integer nextDueMeter;
    private LocalDate lastGeneratedDueDate;
    private Boolean active;
    private List<ChecklistItemDto> checklistItems;
}
