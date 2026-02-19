package com.example.eam.WorkOrder.Repository;

import com.example.eam.WorkOrder.Entity.WorkOrder;
import com.example.eam.WorkOrder.Entity.WorkOrderLaborEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WorkOrderLaborEntryRepository extends JpaRepository<WorkOrderLaborEntry, Long> {

    List<WorkOrderLaborEntry> findByWorkOrder_Id(Long workOrderId);

    void deleteByWorkOrder(WorkOrder workOrder);

    @Query("""
        select le.workOrder.id,
               coalesce(sum(le.laborCost), 0),
               coalesce(sum(le.laborHours), 0),
               coalesce(avg(le.hourlyRate), 0)
        from WorkOrderLaborEntry le
        where le.workOrder.id in :workOrderIds
        group by le.workOrder.id
    """)
    List<Object[]> sumLaborByWorkOrderIds(@Param("workOrderIds") Collection<Long> workOrderIds);
}
