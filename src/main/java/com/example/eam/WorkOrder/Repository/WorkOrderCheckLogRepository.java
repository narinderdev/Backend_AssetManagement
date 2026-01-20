package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrderCheckLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderCheckLogRepository extends JpaRepository<WorkOrderCheckLog, Long> {
    List<WorkOrderCheckLog> findByWorkOrder_Id(Long workOrderId);

    java.util.Optional<WorkOrderCheckLog> findFirstByWorkOrder_IdAndCheckOutAtIsNullOrderByCheckInAtDesc(Long workOrderId);

    java.util.Optional<WorkOrderCheckLog> findFirstByWorkOrder_IdAndTechnician_IdAndCheckOutAtIsNullOrderByCheckInAtDesc(Long workOrderId, Long technicianId);

    List<WorkOrderCheckLog> findByWorkOrder_IdAndCheckOutAtIsNull(Long workOrderId);

    default WorkOrderCheckLog requireOpenLog(Long workOrderId) {
        return findFirstByWorkOrder_IdAndCheckOutAtIsNullOrderByCheckInAtDesc(workOrderId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "No open check-in found for this work order"));
    }

    default WorkOrderCheckLog requireOpenLogForTechnician(Long workOrderId, Long technicianId) {
        return findFirstByWorkOrder_IdAndTechnician_IdAndCheckOutAtIsNullOrderByCheckInAtDesc(workOrderId, technicianId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "No open check-in found for this technician on the work order"));
    }
}
