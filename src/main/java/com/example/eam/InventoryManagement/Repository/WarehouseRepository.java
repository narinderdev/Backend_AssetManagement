package com.example.eam.InventoryManagement.Repository;

import com.example.eam.InventoryManagement.Entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
    boolean existsByNameIgnoreCaseAndDeletedFalseAndCompanyId(String name, Long companyId);

    Optional<Warehouse> findByNameIgnoreCaseAndDeletedFalse(String name);
    Optional<Warehouse> findByNameIgnoreCaseAndDeletedFalseAndCompanyId(String name, Long companyId);

    List<Warehouse> findByActiveTrueAndDeletedFalse();
    List<Warehouse> findByActiveTrueAndDeletedFalseAndCompanyId(Long companyId);

    Optional<Warehouse> findByIdAndDeletedFalseAndCompanyId(Long id, Long companyId);
}
