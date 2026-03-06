package com.example.eam.InventoryManagement.Repository;

import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.Enum.InventoryTransactionType;
import com.example.eam.InventoryManagement.Entity.InventoryAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface InventoryAuditLogRepository extends JpaRepository<InventoryAuditLog, Long> {
    Page<InventoryAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<InventoryAuditLog> findByInventoryItem_IdOrderByCreatedAtDesc(Long itemId, Pageable pageable);
    @EntityGraph(attributePaths = "inventoryItem")
    Page<InventoryAuditLog> findByInventoryItem_SkuNumberContainingIgnoreCaseOrderByCreatedAtDesc(String skuNumber, Pageable pageable);
    Page<InventoryAuditLog> findByTransactionTypeOrderByCreatedAtDesc(InventoryTransactionType type, Pageable pageable);
    Page<InventoryAuditLog> findByReferenceTypeOrderByCreatedAtDesc(InventoryReferenceType refType, Pageable pageable);
    Page<InventoryAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(java.time.LocalDateTime from, java.time.LocalDateTime to, Pageable pageable);
}
