package com.example.eam.InventoryManagement.Repository;

import com.example.eam.InventoryManagement.Entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    Optional<Warehouse> findByNameIgnoreCaseAndDeletedFalse(String name);

    List<Warehouse> findByActiveTrueAndDeletedFalse();
}
