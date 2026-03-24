package com.example.eam.InventoryManagement.Repository;

import com.example.eam.InventoryManagement.Entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    boolean existsByItemId(String itemId);
    boolean existsByItemIdAndCompanyId(String itemId, Long companyId);

    boolean existsBySkuNumberAndDeletedFalse(String skuNumber);
    boolean existsBySkuNumberAndDeletedFalseAndCompanyId(String skuNumber, Long companyId);

    Optional<InventoryItem> findByIdAndDeletedFalse(Long id);
    Optional<InventoryItem> findByIdAndDeletedFalseAndCompanyId(Long id, Long companyId);

    Optional<InventoryItem> findByItemIdAndDeletedFalse(String itemId);
    Optional<InventoryItem> findByItemIdAndDeletedFalseAndCompanyId(String itemId, Long companyId);

    Optional<InventoryItem> findBySkuNumberAndDeletedFalse(String skuNumber);
    Optional<InventoryItem> findBySkuNumberAndDeletedFalseAndCompanyId(String skuNumber, Long companyId);

    Page<InventoryItem> findByDeletedFalse(Pageable pageable);
    Page<InventoryItem> findByDeletedFalseAndCompanyId(Long companyId, Pageable pageable);
}

