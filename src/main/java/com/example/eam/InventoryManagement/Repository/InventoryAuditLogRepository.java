package com.example.eam.InventoryManagement.Repository;

import com.example.eam.Enum.InventoryReferenceType;
import com.example.eam.Enum.InventoryTransactionType;
import com.example.eam.InventoryManagement.Entity.InventoryAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryAuditLogRepository extends JpaRepository<InventoryAuditLog, Long> {
    Page<InventoryAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<InventoryAuditLog> findByInventoryItem_CompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);
    Page<InventoryAuditLog> findByInventoryItem_IdOrderByCreatedAtDesc(Long itemId, Pageable pageable);
    Page<InventoryAuditLog> findByInventoryItem_IdAndInventoryItem_CompanyIdOrderByCreatedAtDesc(Long itemId, Long companyId, Pageable pageable);
    @EntityGraph(attributePaths = "inventoryItem")
    @Query(
            value = """
                select log
                from InventoryAuditLog log
                join log.inventoryItem item
                where lower(item.skuNumber) like lower(concat('%', :skuNumber, '%'))
                order by log.createdAt desc
                """,
            countQuery = """
                select count(log)
                from InventoryAuditLog log
                join log.inventoryItem item
                where lower(item.skuNumber) like lower(concat('%', :skuNumber, '%'))
                """
    )
    Page<InventoryAuditLog> searchBySku(@Param("skuNumber") String skuNumber, Pageable pageable);
    @EntityGraph(attributePaths = "inventoryItem")
    @Query(
            value = """
                select log
                from InventoryAuditLog log
                join log.inventoryItem item
                where item.companyId = :companyId
                  and lower(item.skuNumber) like lower(concat('%', :skuNumber, '%'))
                order by log.createdAt desc
                """,
            countQuery = """
                select count(log)
                from InventoryAuditLog log
                join log.inventoryItem item
                where item.companyId = :companyId
                  and lower(item.skuNumber) like lower(concat('%', :skuNumber, '%'))
                """
    )
    Page<InventoryAuditLog> searchBySkuAndCompanyId(@Param("skuNumber") String skuNumber,
                                                    @Param("companyId") Long companyId,
                                                    Pageable pageable);
    Page<InventoryAuditLog> findByTransactionTypeOrderByCreatedAtDesc(InventoryTransactionType type, Pageable pageable);
    Page<InventoryAuditLog> findByTransactionTypeAndInventoryItem_CompanyIdOrderByCreatedAtDesc(InventoryTransactionType type, Long companyId, Pageable pageable);
    Page<InventoryAuditLog> findByReferenceTypeOrderByCreatedAtDesc(InventoryReferenceType refType, Pageable pageable);
    Page<InventoryAuditLog> findByReferenceTypeAndInventoryItem_CompanyIdOrderByCreatedAtDesc(InventoryReferenceType refType, Long companyId, Pageable pageable);
    Page<InventoryAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(java.time.LocalDateTime from, java.time.LocalDateTime to, Pageable pageable);
    Page<InventoryAuditLog> findByCreatedAtBetweenAndInventoryItem_CompanyIdOrderByCreatedAtDesc(java.time.LocalDateTime from,
                                                                                                   java.time.LocalDateTime to,
                                                                                                   Long companyId,
                                                                                                   Pageable pageable);
}
