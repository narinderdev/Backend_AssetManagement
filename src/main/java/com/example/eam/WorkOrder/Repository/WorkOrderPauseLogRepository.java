package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrderPauseLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderPauseLogRepository extends JpaRepository<WorkOrderPauseLog, Long> {

    Optional<WorkOrderPauseLog> findFirstByCheckLog_IdAndResumeAtIsNullOrderByPauseAtDesc(Long checkLogId);

    List<WorkOrderPauseLog> findByCheckLog_IdOrderByPauseAtAsc(Long checkLogId);
}
