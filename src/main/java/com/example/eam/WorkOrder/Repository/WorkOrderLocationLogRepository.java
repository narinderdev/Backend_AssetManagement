package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrderLocationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WorkOrderLocationLogRepository extends JpaRepository<WorkOrderLocationLog, Long> {

    Optional<WorkOrderLocationLog> findTopByWorkOrder_IdAndTechnician_IdOrderByObservedAtDescIdDesc(Long workOrderId,
                                                                                                      Long technicianId);

    long deleteByObservedAtBefore(LocalDateTime threshold);
}
