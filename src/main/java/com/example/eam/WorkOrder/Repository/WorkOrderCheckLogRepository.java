package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrderCheckLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderCheckLogRepository extends JpaRepository<WorkOrderCheckLog, Long> {
    List<WorkOrderCheckLog> findByWorkOrder_Id(Long workOrderId);
}
