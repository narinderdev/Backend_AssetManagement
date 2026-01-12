package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Entity.WorkOrderChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderChecklistItemRepository extends JpaRepository<WorkOrderChecklistItem, Long> {
    List<WorkOrderChecklistItem> findByWorkOrder_Id(Long workOrderId);
    void deleteByWorkOrder(WorkOrder workOrder);
}
