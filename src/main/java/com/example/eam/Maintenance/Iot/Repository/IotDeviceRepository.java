package com.example.eam.Maintenance.Iot.Repository;

import com.example.eam.Maintenance.Iot.Entity.IotDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IotDeviceRepository extends JpaRepository<IotDevice, Long> {
    Optional<IotDevice> findByCompanyIdAndDeviceUid(Long companyId, String deviceUid);
    Optional<IotDevice> findByIdAndCompanyId(Long id, Long companyId);
    Page<IotDevice> findByCompanyId(Long companyId, Pageable pageable);
    List<IotDevice> findByCompanyId(Long companyId);
    long countByCompanyId(Long companyId);
    long countByCompanyIdAndEnabledTrueAndLastSeenAtAfter(Long companyId, LocalDateTime lastSeenAfter);
}
