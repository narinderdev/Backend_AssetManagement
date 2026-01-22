package com.example.eam.Asset.Repository;

import com.example.eam.Asset.Entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

    Optional<AssetCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<AssetCategory> findAllByOrderByNameAsc();
}
