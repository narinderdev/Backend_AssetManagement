package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Entity.WorkOrderMaterialPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WorkOrderMaterialPlanRepository extends JpaRepository<WorkOrderMaterialPlan, Long> {

    List<WorkOrderMaterialPlan> findByWorkOrder_Id(Long workOrderId);

    void deleteByWorkOrder(WorkOrder workOrder);

    @Query("""
        select mp.workOrder.id, coalesce(sum(mp.totalCostSnapshot), 0)
        from WorkOrderMaterialPlan mp
        where mp.workOrder.id in :workOrderIds
        group by mp.workOrder.id
    """)
    List<Object[]> sumPlannedCostByWorkOrderIds(@Param("workOrderIds") Collection<Long> workOrderIds);
}
