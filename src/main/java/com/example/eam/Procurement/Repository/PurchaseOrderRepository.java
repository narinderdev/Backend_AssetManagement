package com.example.eam.Procurement.Repository;

import com.example.eam.Procurement.Entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    boolean existsByPoNumber(String poNumber);

    boolean existsByMrId(Long mrId);

    List<PurchaseOrder> findByMrId(Long mrId);

    List<PurchaseOrder> findByMrIdIn(Collection<Long> mrIds);

    Optional<PurchaseOrder> findByPoNumber(String poNumber);
}
