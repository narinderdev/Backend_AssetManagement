package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Entity.WorkOrderMaterialUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WorkOrderMaterialUsageRepository extends JpaRepository<WorkOrderMaterialUsage, Long> {

    List<WorkOrderMaterialUsage> findByWorkOrder_Id(Long workOrderId);

    void deleteByWorkOrder(WorkOrder workOrder);

    @Query("""
        select mu.workOrder.id, coalesce(sum(mu.totalCostSnapshot), 0)
        from WorkOrderMaterialUsage mu
        where mu.workOrder.id in :workOrderIds
        group by mu.workOrder.id
    """)
    List<Object[]> sumActualCostByWorkOrderIds(@Param("workOrderIds") Collection<Long> workOrderIds);
}
