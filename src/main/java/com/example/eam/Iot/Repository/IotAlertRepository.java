package com.example.eam.Iot.Repository;

import com.example.eam.Enum.IotAlertStatus;
import com.example.eam.Enum.IotAlertSeverity;
import com.example.eam.Iot.Entity.IotAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface IotAlertRepository extends JpaRepository<IotAlert, Long> {

    Optional<IotAlert> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
            select a
            from IotAlert a
            where a.companyId = :companyId
              and a.asset.id = :assetId
              and a.metricCode = :metricCode
              and a.status in :openStatuses
            order by a.lastTriggeredAt desc
            """)
    java.util.List<IotAlert> findOpenAlertsForMetric(@Param("companyId") Long companyId,
                                                     @Param("assetId") Long assetId,
                                                     @Param("metricCode") String metricCode,
                                                     @Param("openStatuses") Collection<IotAlertStatus> openStatuses,
                                                     Pageable pageable);

    @Query("""
            select a
            from IotAlert a
            where a.companyId = :companyId
              and (:assetId is null or (a.asset is not null and a.asset.id = :assetId))
              and (:location is null or a.location = :location)
              and (:severity is null or a.severity = :severity)
              and (:status is null or a.status = :status)
              and (:fromTime is null or a.occurredAt >= :fromTime)
              and (:toTime is null or a.occurredAt <= :toTime)
            """)
    Page<IotAlert> search(@Param("companyId") Long companyId,
                          @Param("assetId") Long assetId,
                          @Param("location") String location,
                          @Param("severity") IotAlertSeverity severity,
                          @Param("status") IotAlertStatus status,
                          @Param("fromTime") LocalDateTime fromTime,
                          @Param("toTime") LocalDateTime toTime,
                          Pageable pageable);

    long countByCompanyIdAndStatusIn(Long companyId, Collection<IotAlertStatus> statuses);

    java.util.List<IotAlert> findByCompanyIdOrderByOccurredAtDesc(Long companyId, Pageable pageable);

    java.util.List<IotAlert> findByStatusInAndLastNormalAtIsNotNullAndHealthyStreakGreaterThanEqual(
            Collection<IotAlertStatus> statuses,
            Integer healthyStreak
    );
}
