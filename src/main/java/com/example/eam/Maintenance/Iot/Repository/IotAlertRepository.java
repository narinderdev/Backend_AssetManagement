package com.example.eam.Maintenance.Iot.Repository;

import com.example.eam.Enum.MeterType;
import com.example.eam.Maintenance.Iot.Entity.IotAlert;
import com.example.eam.Maintenance.Iot.Enum.IotAlertSeverity;
import com.example.eam.Maintenance.Iot.Enum.IotAlertStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IotAlertRepository extends JpaRepository<IotAlert, Long> {
    Optional<IotAlert> findByIdAndCompanyId(Long id, Long companyId);

    Optional<IotAlert> findTopByCompanyIdAndDevice_IdAndMeterTypeAndStatusOrderByOccurredAtDesc(
            Long companyId,
            Long deviceId,
            MeterType meterType,
            IotAlertStatus status
    );

    long countByCompanyIdAndStatus(Long companyId, IotAlertStatus status);

    @Query("""
            select a from IotAlert a
            where a.companyId = :companyId
              and (:assetId is null or a.asset.id = :assetId)
              and (:location is null or lower(a.location) = lower(:location))
              and (:severity is null or a.severity = :severity)
              and (:status is null or a.status = :status)
            order by a.occurredAt desc
            """)
    List<IotAlert> findFiltered(
            @Param("companyId") Long companyId,
            @Param("assetId") Long assetId,
            @Param("location") String location,
            @Param("severity") IotAlertSeverity severity,
            @Param("status") IotAlertStatus status,
            Pageable pageable
    );

    @Query("""
            select a from IotAlert a
            where a.companyId = :companyId
              and a.device.id = :deviceId
              and a.meterType = :meterType
              and a.status = :status
              and a.occurredAt >= :fromTs
            order by a.occurredAt desc
            """)
    List<IotAlert> findRecentByDeviceAndMeterAndStatus(
            @Param("companyId") Long companyId,
            @Param("deviceId") Long deviceId,
            @Param("meterType") MeterType meterType,
            @Param("status") IotAlertStatus status,
            @Param("fromTs") LocalDateTime fromTs
    );
}
