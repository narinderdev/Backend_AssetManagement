package com.example.eam.Procurement.Repository;

import com.example.eam.Procurement.Entity.MaterialRequisition;
import com.example.eam.Procurement.Enum.MaterialRequisitionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface MaterialRequisitionRepository extends JpaRepository<MaterialRequisition, Long> {

    boolean existsByMrNumber(String mrNumber);
    boolean existsByMrNumberAndCompanyId(String mrNumber, Long companyId);

    Optional<MaterialRequisition> findByMrNumber(String mrNumber);
    Optional<MaterialRequisition> findByMrNumberAndCompanyId(String mrNumber, Long companyId);
    Optional<MaterialRequisition> findByIdAndCompanyId(Long id, Long companyId);

    Page<MaterialRequisition> findByStatus(MaterialRequisitionStatus status, Pageable pageable);
    Page<MaterialRequisition> findByStatusAndCompanyId(MaterialRequisitionStatus status, Long companyId, Pageable pageable);
    Page<MaterialRequisition> findByCompanyId(Long companyId, Pageable pageable);

    long countByStatusIn(Collection<MaterialRequisitionStatus> statuses);
    long countByStatusInAndCompanyId(Collection<MaterialRequisitionStatus> statuses, Long companyId);

    long countByStatusInAndCreatedAtBetween(Collection<MaterialRequisitionStatus> statuses,
                                            Instant start,
                                            Instant end);
    long countByStatusInAndCreatedAtBetweenAndCompanyId(Collection<MaterialRequisitionStatus> statuses,
                                                        Instant start,
                                                        Instant end,
                                                        Long companyId);
}
