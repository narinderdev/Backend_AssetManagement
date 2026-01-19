package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrderTechnicianDailyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WorkOrderTechnicianDailyLogRepository extends JpaRepository<WorkOrderTechnicianDailyLog, Long> {
    Optional<WorkOrderTechnicianDailyLog> findByWorkOrder_IdAndTechnician_IdAndWorkDate(Long workOrderId, Long technicianId, LocalDate workDate);
}
