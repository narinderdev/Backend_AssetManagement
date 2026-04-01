package com.example.eam.Maintenance.Iot.Repository;

import com.example.eam.Enum.MeterType;
import com.example.eam.Maintenance.Iot.Entity.IotTelemetryLog;
import com.example.eam.Maintenance.Iot.Enum.IotAlertSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface IotTelemetryLogRepository extends JpaRepository<IotTelemetryLog, Long> {
    Optional<IotTelemetryLog> findTopByDevice_IdAndMeterTypeAndReadingTimeBeforeOrderByReadingTimeDesc(
            Long deviceId,
            MeterType meterType,
            LocalDateTime readingTime
    );

    long countByDevice_IdAndMeterTypeAndReadingTimeAfterAndSeverityIn(
            Long deviceId,
            MeterType meterType,
            LocalDateTime readingTime,
            Collection<IotAlertSeverity> severities
    );
}
