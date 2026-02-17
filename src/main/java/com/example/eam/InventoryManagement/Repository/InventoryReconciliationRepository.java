package com.example.eam.InventoryManagement.Repository;

import com.example.eam.InventoryManagement.Entity.InventoryReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryReconciliationRepository extends JpaRepository<InventoryReconciliation, Long> {
    Page<InventoryReconciliation> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
