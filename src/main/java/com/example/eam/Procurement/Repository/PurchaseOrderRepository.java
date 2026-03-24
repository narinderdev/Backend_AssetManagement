package com.example.eam.Procurement.Repository;

import com.example.eam.Procurement.Entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    boolean existsByPoNumber(String poNumber);
    boolean existsByPoNumberAndCompanyId(String poNumber, Long companyId);

    boolean existsByMrId(Long mrId);
    boolean existsByMrIdAndCompanyId(Long mrId, Long companyId);

    List<PurchaseOrder> findByMrId(Long mrId);
    List<PurchaseOrder> findByMrIdAndCompanyId(Long mrId, Long companyId);

    List<PurchaseOrder> findByMrIdIn(Collection<Long> mrIds);
    List<PurchaseOrder> findByMrIdInAndCompanyId(Collection<Long> mrIds, Long companyId);

    Optional<PurchaseOrder> findByPoNumber(String poNumber);
    Optional<PurchaseOrder> findByPoNumberAndCompanyId(String poNumber, Long companyId);
    Optional<PurchaseOrder> findByIdAndCompanyId(Long id, Long companyId);

    org.springframework.data.domain.Page<PurchaseOrder> findByCompanyId(Long companyId, org.springframework.data.domain.Pageable pageable);
}
