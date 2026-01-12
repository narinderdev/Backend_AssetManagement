package com.example.eam.WorkOrder.Repository;


import com.example.eam.Enum.WorkOrderStatus;
import com.example.eam.WorkOrder.Entity.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    boolean existsByPmPlan_IdAndPmDueDate(Long pmPlanId, LocalDate pmDueDate);

    boolean existsByWorkOrderId(String workOrderId);

    Optional<WorkOrder> findByIdAndDeletedFalse(Long id);

    Page<WorkOrder> findByDeletedFalse(Pageable pageable);

    Optional<WorkOrder> findByLinkedRequest_Id(Long serviceRequestPkId);

    long countByStatusInAndDeletedFalse(Collection<WorkOrderStatus> statuses);

    long countByStatusAndDeletedFalse(WorkOrderStatus status);

    long countByStatusInAndCreatedAtBetweenAndDeletedFalse(Collection<WorkOrderStatus> statuses,
                                                           LocalDateTime start,
                                                           LocalDateTime end);

    long countByTargetCompletionDateBeforeAndStatusInAndDeletedFalse(LocalDate date,
                                                                     Collection<WorkOrderStatus> statuses);

    long countByTargetCompletionDateBetweenAndStatusInAndDeletedFalse(LocalDate start,
                                                                      LocalDate end,
                                                                      Collection<WorkOrderStatus> statuses);

    List<WorkOrder> findTop5ByDeletedFalseOrderByCreatedAtDesc();

    List<WorkOrder> findByDeletedFalseAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
