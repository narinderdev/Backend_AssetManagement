package com.example.eam.InventoryManagement.Repository;

import com.example.eam.InventoryManagement.Entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    boolean existsByItemId(String itemId);

    boolean existsBySkuNumberAndDeletedFalse(String skuNumber);

    Optional<InventoryItem> findByIdAndDeletedFalse(Long id);

    Optional<InventoryItem> findByItemIdAndDeletedFalse(String itemId);

    Optional<InventoryItem> findBySkuNumberAndDeletedFalse(String skuNumber);

    Page<InventoryItem> findByDeletedFalse(Pageable pageable);
}

