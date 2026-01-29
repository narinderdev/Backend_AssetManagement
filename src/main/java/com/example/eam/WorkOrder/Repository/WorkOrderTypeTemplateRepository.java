package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrderTypeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkOrderTypeTemplateRepository extends JpaRepository<WorkOrderTypeTemplate, Long> {
    boolean existsByWorkOrderTypeIgnoreCase(String workOrderType);
    Optional<WorkOrderTypeTemplate> findByWorkOrderTypeIgnoreCase(String workOrderType);
}
