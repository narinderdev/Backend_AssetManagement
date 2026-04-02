package com.example.eam.Iot.Repository;

import com.example.eam.Iot.Entity.IotDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IotDeviceRepository extends JpaRepository<IotDevice, Long> {

    Optional<IotDevice> findByIdAndCompanyId(Long id, Long companyId);

    Page<IotDevice> findByCompanyId(Long companyId, Pageable pageable);

    Optional<IotDevice> findByCompanyIdAndDeviceUid(Long companyId, String deviceUid);

    boolean existsByCompanyIdAndDeviceUid(Long companyId, String deviceUid);

    boolean existsByDeviceUid(String deviceUid);

    Optional<IotDevice> findByDeviceUid(String deviceUid);

    long countByCompanyIdAndEnabledTrue(Long companyId);

    @Query("""
            select count(d)
            from IotDevice d
            where d.companyId = :companyId
              and d.enabled = true
              and d.lastSeenAt is not null
              and d.lastSeenAt >= :threshold
            """)
    long countOnlineEnabledDevices(@Param("companyId") Long companyId, @Param("threshold") LocalDateTime threshold);
}
