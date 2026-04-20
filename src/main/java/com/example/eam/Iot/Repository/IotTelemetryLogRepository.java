package com.example.eam.Iot.Repository;

import com.example.eam.Enum.IngestProcessingStatus;
import com.example.eam.Iot.Entity.IotTelemetryLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IotTelemetryLogRepository extends JpaRepository<IotTelemetryLog, Long> {

    Optional<IotTelemetryLog> findByCompanyIdAndDevice_IdAndEventId(Long companyId, Long deviceId, String eventId);

    List<IotTelemetryLog> findByCompanyIdAndAsset_IdAndMetricCodeOrderByObservedAtDesc(Long companyId, Long assetId, String metricCode, Pageable pageable);

    List<IotTelemetryLog> findByIngestStatusInOrderByCreatedAtAsc(List<IngestProcessingStatus> statuses, Pageable pageable);

    long deleteByObservedAtBefore(LocalDateTime threshold);
}

