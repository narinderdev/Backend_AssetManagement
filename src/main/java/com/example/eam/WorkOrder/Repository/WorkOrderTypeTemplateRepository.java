package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrderTypeTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkOrderTypeTemplateRepository extends JpaRepository<WorkOrderTypeTemplate, Long> {
    boolean existsByWorkOrderTypeIgnoreCaseAndCompanyId(String workOrderType, Long companyId);
    boolean existsByWorkOrderTypeIgnoreCaseAndCompanyIdAndIdNot(String workOrderType, Long companyId, Long id);
    Optional<WorkOrderTypeTemplate> findByWorkOrderTypeIgnoreCaseAndCompanyId(String workOrderType, Long companyId);
    Optional<WorkOrderTypeTemplate> findByIdAndCompanyId(Long id, Long companyId);
    Page<WorkOrderTypeTemplate> findByCompanyId(Long companyId, Pageable pageable);
}
