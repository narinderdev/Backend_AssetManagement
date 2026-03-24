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
    Optional<ServiceMaintenance> findByIdAndDeletedFalseAndCompanyId(Long id, Long companyId);

    Page<ServiceMaintenance> findByDeletedFalseAndStatus(ServiceRequestStatus status, Pageable pageable);
    Page<ServiceMaintenance> findByDeletedFalseAndStatusAndCompanyId(ServiceRequestStatus status, Long companyId, Pageable pageable);

    Page<ServiceMaintenance> findByDeletedFalseAndAsset_Id(Long assetId, Pageable pageable);
    Page<ServiceMaintenance> findByDeletedFalseAndAsset_IdAndCompanyId(Long assetId, Long companyId, Pageable pageable);

    boolean existsByRequestId(String requestId);
    boolean existsByRequestIdAndCompanyId(String requestId, Long companyId);

    Optional<ServiceMaintenance> findTopByOrderByIdDesc();   // for auto-numbering
    Optional<ServiceMaintenance> findTopByCompanyIdOrderByIdDesc(Long companyId);

    Page<ServiceMaintenance> findByDeletedFalseAndStatusNot(ServiceRequestStatus status, Pageable pageable);
    Page<ServiceMaintenance> findByDeletedFalseAndStatusNotAndCompanyId(ServiceRequestStatus status, Long companyId, Pageable pageable);

    long countByDeletedFalseAndStatusIn(Collection<ServiceRequestStatus> statuses);
    long countByDeletedFalseAndStatusInAndCompanyId(Collection<ServiceRequestStatus> statuses, Long companyId);

    long countByDeletedFalseAndStatusInAndRequestDateBetween(Collection<ServiceRequestStatus> statuses,
                                              LocalDateTime start,
                                              LocalDateTime end);
    long countByDeletedFalseAndStatusInAndRequestDateBetweenAndCompanyId(Collection<ServiceRequestStatus> statuses,
                                                                         LocalDateTime start,
                                                                         LocalDateTime end,
                                                                         Long companyId);

    List<ServiceMaintenance> findByDeletedFalseAndStatusOrderByRequestDateDesc(ServiceRequestStatus status);
    List<ServiceMaintenance> findByDeletedFalseAndStatusAndCompanyIdOrderByRequestDateDesc(ServiceRequestStatus status, Long companyId);
}
