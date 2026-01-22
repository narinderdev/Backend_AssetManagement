package com.example.eam.ServiceMaintenance.Repository;

import com.example.eam.Enum.ServiceRequestStatus;
import com.example.eam.ServiceMaintenance.Entity.ServiceMaintenance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceMaintenanceRepository extends JpaRepository<ServiceMaintenance, Long> {

    Optional<ServiceMaintenance> findByIdAndDeletedFalse(Long id);

    Page<ServiceMaintenance> findByDeletedFalseAndStatus(ServiceRequestStatus status, Pageable pageable);

    Page<ServiceMaintenance> findByDeletedFalseAndAsset_Id(Long assetId, Pageable pageable);

    boolean existsByRequestId(String requestId);

    Optional<ServiceMaintenance> findTopByOrderByIdDesc();   // for auto-numbering

    Page<ServiceMaintenance> findByDeletedFalseAndStatusNot(ServiceRequestStatus status, Pageable pageable);

    long countByDeletedFalseAndStatusIn(Collection<ServiceRequestStatus> statuses);

    long countByDeletedFalseAndStatusInAndRequestDateBetween(Collection<ServiceRequestStatus> statuses,
                                              LocalDateTime start,
                                              LocalDateTime end);

    List<ServiceMaintenance> findByDeletedFalseAndStatusOrderByRequestDateDesc(ServiceRequestStatus status);
}
